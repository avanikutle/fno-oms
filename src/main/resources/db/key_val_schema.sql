-- 1. History table (same columns + audit columns)
CREATE TABLE public.algo_key_value_history (
    -- audit columns
    audit_action   TEXT        NOT NULL,  -- 'INSERT' | 'UPDATE' | 'DELETE'
    audit_ts       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    audit_user     VARCHAR(50) NOT NULL DEFAULT CURRENT_USER,

    -- original columns
    id             SERIAL4     NOT NULL,
    key_name       VARCHAR(100) NOT NULL,
    key_value      TEXT        NOT NULL,
    "comments"     TEXT        NULL,
    updated_by     VARCHAR(50) DEFAULT 'SYSTEM'::character varying NULL,
    updated_at     TIMESTAMPTZ DEFAULT NOW() NULL,

    -- constraints (mirroring original, but no serial primary key on id)
    CONSTRAINT algo_key_value_history_pkey PRIMARY KEY (id, audit_ts),
    CONSTRAINT algo_key_value_history_key_name_check CHECK (key_name <> '')
);

-- Optional: index on key_name for faster lookup in history
CREATE INDEX algo_key_value_history_key_name_idx
    ON public.algo_key_value_history (key_name);

-- Optional: index on audit_ts
CREATE INDEX algo_key_value_history_audit_ts_idx
    ON public.algo_key_value_history (audit_ts);


-- 2. Trigger function to log changes into history
CREATE OR REPLACE FUNCTION public.log_algo_key_value_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO public.algo_key_value_history (
            audit_action,
            audit_ts,
            audit_user,
            id,
            key_name,
            key_value,
            "comments",
            updated_by,
            updated_at
        )
        VALUES (
            'INSERT',
            NOW(),
            CURRENT_USER,
            NEW.id,
            NEW.key_name,
            NEW.key_value,
            NEW."comments",
            COALESCE(NEW.updated_by, 'SYSTEM'),
            COALESCE(NEW.updated_at, NOW())
        );
        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO public.algo_key_value_history (
            audit_action,
            audit_ts,
            audit_user,
            id,
            key_name,
            key_value,
            "comments",
            updated_by,
            updated_at
        )
        VALUES (
            'UPDATE',
            NOW(),
            CURRENT_USER,
            NEW.id,
            NEW.key_name,
            NEW.key_value,
            NEW."comments",
            COALESCE(NEW.updated_by, 'SYSTEM'),
            COALESCE(NEW.updated_at, NOW())
        );
        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO public.algo_key_value_history (
            audit_action,
            audit_ts,
            audit_user,
            id,
            key_name,
            key_value,
            "comments",
            updated_by,
            updated_at
        )
        VALUES (
            'DELETE',
            NOW(),
            CURRENT_USER,
            OLD.id,
            OLD.key_name,
            OLD.key_value,
            OLD."comments",
            COALESCE(OLD.updated_by, 'SYSTEM'),
            COALESCE(OLD.updated_at, NOW())
        );
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;


-- 3. Attach trigger to original table
CREATE TRIGGER trg_algo_key_value_history
AFTER INSERT OR UPDATE OR DELETE ON public.algo_key_value
FOR EACH ROW EXECUTE FUNCTION public.log_algo_key_value_history();
