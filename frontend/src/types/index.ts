// API Response types

export interface User {
  id: string;
  email: string;
  full_name: string | null;
  company_name: string | null;
  is_active: boolean;
  is_admin: boolean;
  created_at: string;
  updated_at: string;
}

export interface Token {
  access_token: string;
  token_type: string;
}

export interface Tender {
  id: string;
  source_id: string;
  pdf_url: string;
  pdf_path: string | null;
  title: string | null;
  reference: string | null;
  deadline: string | null;
  estimated_value: number | null;
  requirements_summary: string | null;
  contact_name: string | null;
  contact_email: string | null;
  extraction_confidence: number | null;
  discovered_at: string;
  extracted_at: string | null;
  score: number | null;
  relevance_category: string | null;
  score_explanation: string | null;
}

export interface TenderListItem {
  id: string;
  source_id: string;
  title: string | null;
  reference: string | null;
  deadline: string | null;
  estimated_value: number | null;
  discovered_at: string;
  score: number | null;
  relevance_category: string | null;
}

export interface TenderList {
  items: TenderListItem[];
  total: number;
  page: number;
  per_page: number;
  pages: number;
}

export interface TenderFilters {
  search?: string;
  source_id?: string;
  min_score?: number;
  deadline_from?: string;
  deadline_to?: string;
  sort_by?: string;
  sort_order?: 'asc' | 'desc';
  page?: number;
  per_page?: number;
}

export interface DashboardStats {
  total_tenders: number;
  high_priority_count: number;
  avg_score: number | null;
  tenders_this_week: number;
  sources_active: number;
}

export interface RecentTender {
  id: string;
  title: string | null;
  reference: string | null;
  source_id: string;
  deadline: string | null;
  estimated_value: number | null;
  score: number;
  relevance_category: string;
  discovered_at: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterData {
  email: string;
  password: string;
  full_name?: string;
  company_name?: string;
}

// Pipeline types
export type JobType = 'discover' | 'download' | 'extract' | 'score' | 'full_pipeline';
export type JobStatus = 'pending' | 'running' | 'completed' | 'failed';

export interface Job {
  id: string;
  job_type: JobType;
  source_id: string | null;
  status: JobStatus;
  progress: number;
  total_items: number | null;
  processed_items: number;
  error_message: string | null;
  started_at: string | null;
  completed_at: string | null;
  created_at: string;
}

export interface JobLog {
  id: string;
  job_id: string;
  level: 'debug' | 'info' | 'warning' | 'error';
  message: string;
  extra_data: Record<string, unknown> | null;
  created_at: string;
}

export interface JobDetail extends Job {
  logs: JobLog[];
}

// Config types
export interface ValueRange {
  min: number;
  max: number | null;
}

export interface Criteria {
  id: string;
  user_id: string;
  name: string;
  capabilities: string[];
  preferred_sectors: string[];
  preferred_locations: string[];
  value_range: ValueRange;
  keywords: string[];
  weights: Record<string, number>;
  is_default: boolean;
  created_at: string;
  updated_at: string;
}

export interface CriteriaCreate {
  name: string;
  capabilities?: string[];
  preferred_sectors?: string[];
  preferred_locations?: string[];
  value_range?: ValueRange;
  keywords?: string[];
  weights?: Record<string, number>;
  is_default?: boolean;
}

export interface CriteriaUpdate {
  name?: string;
  capabilities?: string[];
  preferred_sectors?: string[];
  preferred_locations?: string[];
  value_range?: ValueRange;
  keywords?: string[];
  weights?: Record<string, number>;
  is_default?: boolean;
}

export interface Source {
  id: string;
  name: string;
  url: string;
  config_path: string | null;
  is_active: boolean;
  last_scraped_at: string | null;
  created_at: string;
}
