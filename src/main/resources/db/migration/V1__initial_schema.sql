CREATE TABLE identity_account (
    actor_id uuid PRIMARY KEY,
    username text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    enabled boolean NOT NULL,
    requester boolean NOT NULL,
    reviewer boolean NOT NULL,
    access_version bigint NOT NULL DEFAULT 0
);

CREATE TABLE change_request (
    id uuid PRIMARY KEY,
    title text,
    description text,
    reason text,
    requester_id uuid NOT NULL REFERENCES identity_account(actor_id),
    status text NOT NULL CHECK (status IN ('Draft','PendingReview','Approved','ChangesRequested')),
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX change_request_requester_id_id ON change_request(requester_id,id);
CREATE INDEX change_request_status_id ON change_request(status,id);

CREATE TABLE status_history (
    id uuid PRIMARY KEY,
    request_id uuid NOT NULL REFERENCES change_request(id) ON DELETE RESTRICT,
    actor_id uuid NOT NULL REFERENCES identity_account(actor_id),
    occurred_at timestamptz NOT NULL,
    from_status text NOT NULL,
    to_status text NOT NULL,
    explanation text NOT NULL,
    resulting_version bigint NOT NULL,
    UNIQUE(request_id,resulting_version),
    CHECK ((from_status='Draft' AND to_status='PendingReview')
        OR (from_status='ChangesRequested' AND to_status='PendingReview')
        OR (from_status='PendingReview' AND to_status IN ('Approved','ChangesRequested'))),
    -- Java Character.isWhitespace/isSpaceChar code points, including Unicode separators.
    CHECK (to_status <> 'ChangesRequested' OR length(translate(explanation,
        U&'\0009\000A\000B\000C\000D\001C\001D\001E\001F\0020\00A0\1680\2000\2001\2002\2003\2004\2005\2006\2007\2008\2009\200A\2028\2029\202F\205F\3000','')) > 0)
);
