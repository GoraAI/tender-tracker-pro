import { useCallback, useEffect, useRef, useState } from 'react';
import type { JobStatus } from '../types';

interface JobStatusUpdate {
  id: string;
  status: JobStatus;
  progress: number;
  total_items: number | null;
  processed_items: number;
  error_message: string | null;
}

interface WebSocketMessage {
  type: 'status' | 'log' | 'complete' | 'error';
  data?: JobStatusUpdate | { id: string; level: string; message: string; timestamp: string } | { status: JobStatus };
  message?: string;
}

interface UseWebSocketOptions {
  onStatusUpdate?: (status: JobStatusUpdate) => void;
  onLog?: (log: { id: string; level: string; message: string; timestamp: string }) => void;
  onComplete?: (status: JobStatus) => void;
  onError?: (error: string) => void;
}

export const useWebSocket = (jobId: string | null, options: UseWebSocketOptions = {}) => {
  const [isConnected, setIsConnected] = useState(false);
  const [logs, setLogs] = useState<{ id: string; level: string; message: string; timestamp: string }[]>([]);
  const [status, setStatus] = useState<JobStatusUpdate | null>(null);
  const [error, setError] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);

  const connect = useCallback(() => {
    if (!jobId) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const url = `${protocol}//${host}/ws/pipeline/jobs/${jobId}/stream`;

    try {
      const ws = new WebSocket(url);

      ws.onopen = () => {
        setIsConnected(true);
        setError(null);
      };

      ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data);

          switch (message.type) {
            case 'status':
              if (message.data && 'status' in message.data && 'progress' in message.data) {
                setStatus(message.data as JobStatusUpdate);
                options.onStatusUpdate?.(message.data as JobStatusUpdate);
              }
              break;

            case 'log':
              if (message.data && 'message' in message.data && 'level' in message.data) {
                const logData = message.data as { id: string; level: string; message: string; timestamp: string };
                setLogs((prev) => [...prev, logData]);
                options.onLog?.(logData);
              }
              break;

            case 'complete':
              if (message.data && 'status' in message.data) {
                options.onComplete?.((message.data as { status: JobStatus }).status);
              }
              break;

            case 'error':
              setError(message.message || 'Unknown error');
              options.onError?.(message.message || 'Unknown error');
              break;
          }
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e);
        }
      };

      ws.onclose = () => {
        setIsConnected(false);
        wsRef.current = null;

        // Attempt to reconnect after 3 seconds
        if (status?.status === 'running') {
          reconnectTimeoutRef.current = window.setTimeout(() => {
            connect();
          }, 3000);
        }
      };

      ws.onerror = () => {
        setError('WebSocket connection error');
        ws.close();
      };

      wsRef.current = ws;
    } catch (e) {
      setError('Failed to connect to WebSocket');
    }
  }, [jobId, options, status?.status]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }

    setIsConnected(false);
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  useEffect(() => {
    if (jobId) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [jobId, connect, disconnect]);

  return {
    isConnected,
    logs,
    status,
    error,
    connect,
    disconnect,
    clearLogs,
  };
};
