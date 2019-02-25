CREATE TABLE public.account (
    id uuid NOT NULL,
    account_type_id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.account_entry (
    id uuid NOT NULL,
    account_id uuid NOT NULL,
    amount integer NOT NULL,
    credit boolean NOT NULL,
    value_date timestamp without time zone NOT NULL,
    transaction_id uuid NOT NULL
);

CREATE TABLE public.account_party (
    account_id uuid NOT NULL,
    party_id uuid NOT NULL,
    party_role_id uuid NOT NULL
);

CREATE TABLE public.account_type (
    id uuid NOT NULL,
    name text NOT NULL,
    description text NOT NULL
);

CREATE TABLE public.contract (
    id uuid NOT NULL,
    contract_type_id uuid NOT NULL,
    valid_from date NOT NULL,
    valid_through date,
    product_id uuid NOT NULL
);

CREATE TABLE public.contract_party (
    contract_id uuid NOT NULL,
    party_role_id uuid NOT NULL,
    party_id uuid NOT NULL,
    share real
);

CREATE TABLE public.contract_type (
    id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.currency (
    id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.party (
    id uuid NOT NULL,
    party_type_id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.party_type (
    id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.event_log (
    id uuid NOT NULL,
    at timestamp without time zone NOT NULL,
    data json NOT NULL
);

CREATE TABLE public.invoice (
    id uuid NOT NULL,
    contract_id uuid NOT NULL,
    amount real NOT NULL,
    created_date date NOT NULL,
    due_date date NOT NULL,
    payment_reference_id uuid NOT NULL
);

CREATE TABLE public.loan (
    id uuid NOT NULL,
    account_id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    loan_type_id uuid NOT NULL,
    contract_id uuid NOT NULL,
    principal integer NOT NULL,
    currency_id uuid NOT NULL
);

CREATE TABLE public.payment_plan_item (
    id uuid NOT NULL,
    payment_plan_id uuid NOT NULL,
    amortization integer NOT NULL,
    interest integer NOT NULL
);

CREATE TABLE public.payment_structure (
    id uuid NOT NULL,
    loan_id uuid NOT NULL,
    type_id uuid NOT NULL
);

COMMENT ON TABLE public.payment_structure IS 'Abstract recursive description of executing the loan obligation until finalized.';

COMMENT ON COLUMN public.payment_structure.loan_id IS 'To be changed to contract_id';

CREATE TABLE public.payment_structure_term (
    id uuid NOT NULL,
    payment_structure_id uuid NOT NULL,
    type_id uuid NOT NULL,
    value integer,
    applies_after interval,
    applies_for interval
);

COMMENT ON COLUMN public.payment_structure_term.type_id IS 'Types the value column';

CREATE TABLE public.payment_structure_term_type (
    id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.loan_type (
    id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.party_role (
    id uuid NOT NULL,
    name text NOT NULL,
    description text NOT NULL
);

CREATE TABLE public.payment_plan (
    id uuid NOT NULL,
    loan_id uuid NOT NULL
);

COMMENT ON TABLE public.payment_plan IS 'The concrete plan with one item per invoicable period.';

COMMENT ON COLUMN public.payment_plan.loan_id IS 'Also to be changed to contract_id, as per the idea fo payment_structure.loan_id ?';

CREATE TABLE public.payment_structure_type (
    id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.product (
    id uuid NOT NULL,
    name text NOT NULL,
    product_type_id uuid NOT NULL
);

CREATE TABLE public.product_type (
    id uuid NOT NULL,
    name text NOT NULL
);

CREATE TABLE public.reference (
    id uuid NOT NULL,
    image text NOT NULL,
    create_date date NOT NULL
);

CREATE TABLE public.schedule_type (
    id uuid NOT NULL,
    name text
);

CREATE TABLE public.transaction (
    id uuid NOT NULL,
    amount integer NOT NULL,
    currency_id uuid NOT NULL,
    value_date timestamp without time zone NOT NULL,
    transaction_type_id uuid NOT NULL
);

CREATE TABLE public.transaction_type (
    id uuid NOT NULL,
    name text NOT NULL
);


ALTER TABLE ONLY public.account_entry
    ADD CONSTRAINT account_entry_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.account_party
    ADD CONSTRAINT account_party_pkey PRIMARY KEY (account_id);

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.account_type
    ADD CONSTRAINT account_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.contract_party
    ADD CONSTRAINT contract_party_pkey PRIMARY KEY (contract_id, party_role_id, party_id);

ALTER TABLE ONLY public.contract
    ADD CONSTRAINT contract_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.contract_type
    ADD CONSTRAINT contract_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.currency
    ADD CONSTRAINT currency_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.event_log
    ADD CONSTRAINT event_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT invoice_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_structure_term_type
    ADD CONSTRAINT loan_payment_structure_item_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_structure
    ADD CONSTRAINT loan_payment_structure_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.loan
    ADD CONSTRAINT loan_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.loan_type
    ADD CONSTRAINT loan_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.party
    ADD CONSTRAINT party_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.party_role
    ADD CONSTRAINT party_role_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.party_type
    ADD CONSTRAINT party_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_plan_item
    ADD CONSTRAINT payment_plan_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_plan
    ADD CONSTRAINT payment_plan_loan_id_key UNIQUE (loan_id);

ALTER TABLE ONLY public.payment_plan
    ADD CONSTRAINT payment_plan_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_structure_term
    ADD CONSTRAINT payment_structure_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.payment_structure
    ADD CONSTRAINT payment_structure_loan_id_key UNIQUE (loan_id);

ALTER TABLE ONLY public.payment_structure_type
    ADD CONSTRAINT payment_structure_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_type
    ADD CONSTRAINT product_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.reference
    ADD CONSTRAINT reference_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.schedule_type
    ADD CONSTRAINT schedule_type_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.transaction_type
    ADD CONSTRAINT transaction_type_pkey PRIMARY KEY (id);


CREATE INDEX event_log_at_idx ON public.event_log USING btree (at);