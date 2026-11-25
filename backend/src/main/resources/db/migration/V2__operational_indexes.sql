CREATE INDEX IF NOT EXISTS idx_restaurants_owner ON restaurants (owner_id);
CREATE INDEX IF NOT EXISTS idx_branches_restaurant ON branches (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_assignments_branch ON branch_assignments (branch_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expiry ON sessions (expires_at);
CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_summary_created ON ai_summaries (created_at);
