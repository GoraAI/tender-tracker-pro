import { useEffect, useRef } from 'react';
import { Box, IconButton, Typography } from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { GlassCard } from '../common/GlassCard';

interface LogEntry {
  id: string;
  level: string;
  message: string;
  timestamp: string;
}

interface JobLogsProps {
  logs: LogEntry[];
  maxHeight?: number;
  autoScroll?: boolean;
}

const getLevelColor = (level: string) => {
  switch (level) {
    case 'error':
      return '#EF4444';
    case 'warning':
      return '#F59E0B';
    case 'debug':
      return '#64748B';
    default:
      return '#E2E8F0';
  }
};

export const JobLogs = ({ logs, maxHeight = 300, autoScroll = true }: JobLogsProps) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (autoScroll && containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  const handleCopy = () => {
    const text = logs
      .map((log) => `[${log.timestamp}] [${log.level.toUpperCase()}] ${log.message}`)
      .join('\n');
    navigator.clipboard.writeText(text);
  };

  const formatTimestamp = (timestamp: string) => {
    try {
      return new Date(timestamp).toLocaleTimeString('en-ZA', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return timestamp;
    }
  };

  return (
    <GlassCard sx={{ p: 0 }}>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          p: 2,
          borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
        }}
      >
        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
          Job Logs
        </Typography>
        <IconButton
          size="small"
          onClick={handleCopy}
          sx={{ color: 'text.secondary' }}
        >
          <ContentCopyIcon fontSize="small" />
        </IconButton>
      </Box>

      <Box
        ref={containerRef}
        sx={{
          maxHeight,
          overflow: 'auto',
          fontFamily: 'monospace',
          fontSize: '0.8rem',
          p: 2,
          '&::-webkit-scrollbar': {
            width: 8,
          },
          '&::-webkit-scrollbar-track': {
            background: 'rgba(255, 255, 255, 0.05)',
          },
          '&::-webkit-scrollbar-thumb': {
            background: 'rgba(255, 255, 255, 0.2)',
            borderRadius: 4,
          },
        }}
      >
        {logs.length === 0 ? (
          <Typography
            variant="body2"
            sx={{ color: 'text.secondary', fontStyle: 'italic' }}
          >
            No logs yet...
          </Typography>
        ) : (
          logs.map((log) => (
            <Box
              key={log.id}
              sx={{
                display: 'flex',
                gap: 1,
                mb: 0.5,
                '&:last-child': { mb: 0 },
              }}
            >
              <Typography
                component="span"
                sx={{ color: 'text.secondary', flexShrink: 0 }}
              >
                {formatTimestamp(log.timestamp)}
              </Typography>
              <Typography
                component="span"
                sx={{
                  color: getLevelColor(log.level),
                  fontWeight: log.level === 'error' ? 600 : 400,
                  flexShrink: 0,
                  width: 60,
                }}
              >
                [{log.level.toUpperCase()}]
              </Typography>
              <Typography
                component="span"
                sx={{ color: getLevelColor(log.level), wordBreak: 'break-word' }}
              >
                {log.message}
              </Typography>
            </Box>
          ))
        )}
      </Box>
    </GlassCard>
  );
};
