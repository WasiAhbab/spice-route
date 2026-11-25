create table users(id uuid primary key, name text not null, email text unique not null, password_hash text not null, role text not null check(role in ('OWNER','MANAGER','ADMIN')), active boolean not null default true);
create table restaurants(id uuid primary key, name text not null, owner_id uuid not null references users(id), active boolean not null default true);
create table branches(id uuid primary key, restaurant_id uuid not null references restaurants(id), name text not null, address text not null default '', unique(restaurant_id,name), unique(id,restaurant_id));
create table branch_assignments(user_id uuid references users(id), branch_id uuid references branches(id), primary key(user_id,branch_id));
create table restaurant_tables(id uuid primary key, branch_id uuid not null references branches(id), label text not null, token uuid unique not null, active boolean not null default true, unique(branch_id,label));
create table feedback(id uuid primary key, table_id uuid not null references restaurant_tables(id), rating integer not null check(rating between 1 and 5), comment text, customer_name text, phone text, language text not null check(language in ('en','bn','mixed')), created_at timestamptz not null default now());
create index feedback_table_date on feedback(table_id,created_at);
create table sessions(token_hash text primary key, user_id uuid not null references users(id), expires_at timestamptz not null);
create table ai_summaries(id uuid primary key, branch_id uuid not null references branches(id), input_hash text not null, result text not null, created_at timestamptz not null default now(), unique(branch_id,input_hash));
