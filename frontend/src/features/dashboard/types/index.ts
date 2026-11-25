export type Row = {
  id: string;
  name: string;
  restaurant_id?: string;
  label?: string;
  active?: boolean;
  archived?: boolean;
  welcome_message?: string;
  thank_you_message?: string;
  collect_contact?: boolean;
  address?: string;
  platform_active?: boolean;
  review_status?: string;
  internal_note?: string;
  version?: number;
  token?: string;
  email?: string;
  role?: string;
  rating?: number;
  comment?: string;
  created_at?: string;
  table_label?: string;
  language?: string;
  volume?: number;
  average?: number;
};
export type Overview = { restaurants: Row[]; branches: Row[] };
export type Report = {
  sentiment: string;
  commentCount: number;
  topics: {
    title: string;
    kind: string;
    count: number;
    examples: string[];
    suggestion: string;
  }[];
};

export type AdminData = { restaurants: Row[]; users: Row[] };
