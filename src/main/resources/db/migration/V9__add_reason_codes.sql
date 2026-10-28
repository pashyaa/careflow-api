-- BUG-305: configurable resolution / cancellation reason codes + audit columns.
CREATE TABLE reason_codes (
                              id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              category   VARCHAR(20)  NOT NULL,
                              code       VARCHAR(40)  NOT NULL,
                              label      VARCHAR(120) NOT NULL,
                              sort_order INTEGER      NOT NULL DEFAULT 0,
                              active     BOOLEAN      NOT NULL DEFAULT TRUE,
                              CONSTRAINT chk_reason_codes_category CHECK (category IN ('RESOLUTION', 'CANCELLATION')),
                              CONSTRAINT uq_reason_codes_category_code UNIQUE (category, code)
);

INSERT INTO reason_codes (category, code, label, sort_order) VALUES
                                                                 ('RESOLUTION',   'FIXED',              'Fault repaired',                    10),
                                                                 ('RESOLUTION',   'PART_REPLACED',      'Part replaced',                     20),
                                                                 ('RESOLUTION',   'ADJUSTED',           'Adjusted / recalibrated',           30),
                                                                 ('RESOLUTION',   'NO_FAULT_FOUND',     'No fault found',                    40),
                                                                 ('RESOLUTION',   'CUSTOMER_RESOLVED',  'Resolved by customer',              50),
                                                                 ('CANCELLATION', 'DUPLICATE',          'Duplicate work order',              10),
                                                                 ('CANCELLATION', 'RAISED_IN_ERROR',    'Raised in error',                   20),
                                                                 ('CANCELLATION', 'CUSTOMER_CANCELLED', 'Cancelled by customer',             30),
                                                                 ('CANCELLATION', 'NO_LONGER_REQUIRED', 'No longer required',                40),
                                                                 ('CANCELLATION', 'OUT_OF_SCOPE',       'Out of contract scope',             50);

-- Nullable: history rows written before this change (and non-terminal transitions) have no reason.
ALTER TABLE work_order_status_history ADD COLUMN reason_code  VARCHAR(40);
ALTER TABLE work_order_status_history ADD COLUMN reason_label VARCHAR(120);