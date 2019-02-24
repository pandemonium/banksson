--
-- PostgreSQL database dump
--

-- Dumped from database version 11.1
-- Dumped by pg_dump version 11.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: account; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.account (
    id integer NOT NULL,
    account_type_id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.account OWNER TO pa;

--
-- Name: account_entry; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.account_entry (
    id bigint NOT NULL,
    account_id integer NOT NULL,
    amount integer NOT NULL,
    credit boolean NOT NULL,
    value_date timestamp without time zone NOT NULL,
    transaction_id bigint NOT NULL
);


ALTER TABLE public.account_entry OWNER TO pa;

--
-- Name: account_entry_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.account_entry_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.account_entry_id_seq OWNER TO pa;

--
-- Name: account_entry_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.account_entry_id_seq OWNED BY public.account_entry.id;


--
-- Name: account_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.account_id_seq OWNER TO pa;

--
-- Name: account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.account_id_seq OWNED BY public.account.id;


--
-- Name: account_party; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.account_party (
    account_id integer NOT NULL,
    party_id integer NOT NULL,
    party_role_id integer NOT NULL
);


ALTER TABLE public.account_party OWNER TO pa;

--
-- Name: account_party_account_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.account_party_account_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.account_party_account_id_seq OWNER TO pa;

--
-- Name: account_party_account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.account_party_account_id_seq OWNED BY public.account_party.account_id;


--
-- Name: account_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.account_type (
    id integer NOT NULL,
    name text NOT NULL,
    description text DEFAULT '""'::text NOT NULL
);


ALTER TABLE public.account_type OWNER TO pa;

--
-- Name: account_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.account_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.account_type_id_seq OWNER TO pa;

--
-- Name: account_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.account_type_id_seq OWNED BY public.account_type.id;


--
-- Name: contract; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.contract (
    id integer NOT NULL,
    contract_type_id integer NOT NULL,
    valid_from date NOT NULL,
    valid_through date,
    product_id integer NOT NULL
);


ALTER TABLE public.contract OWNER TO pa;

--
-- Name: COLUMN contract.contract_type_id; Type: COMMENT; Schema: public; Owner: pa
--

COMMENT ON COLUMN public.contract.contract_type_id IS 'Perhaps this should include product_id?';


--
-- Name: contract_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.contract_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.contract_id_seq OWNER TO pa;

--
-- Name: contract_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.contract_id_seq OWNED BY public.contract.id;


--
-- Name: contract_party; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.contract_party (
    contract_id integer NOT NULL,
    party_role_id integer NOT NULL,
    party_id integer NOT NULL,
    share real
);


ALTER TABLE public.contract_party OWNER TO pa;

--
-- Name: contract_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.contract_type (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.contract_type OWNER TO pa;

--
-- Name: contract_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.contract_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.contract_type_id_seq OWNER TO pa;

--
-- Name: contract_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.contract_type_id_seq OWNED BY public.contract_type.id;


--
-- Name: currency; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.currency (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.currency OWNER TO pa;

--
-- Name: currency_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.currency_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.currency_id_seq OWNER TO pa;

--
-- Name: currency_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.currency_id_seq OWNED BY public.currency.id;


--
-- Name: party; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.party (
    id integer NOT NULL,
    party_type_id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.party OWNER TO pa;

--
-- Name: entity_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.entity_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.entity_id_seq OWNER TO pa;

--
-- Name: entity_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.entity_id_seq OWNED BY public.party.id;


--
-- Name: party_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.party_type (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.party_type OWNER TO pa;

--
-- Name: entity_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.entity_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.entity_type_id_seq OWNER TO pa;

--
-- Name: entity_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.entity_type_id_seq OWNED BY public.party_type.id;


--
-- Name: invoice; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.invoice (
    id integer NOT NULL,
    contract_id integer NOT NULL,
    amount real NOT NULL,
    created_date date NOT NULL,
    due_date date NOT NULL,
    payment_reference_id integer NOT NULL
);


ALTER TABLE public.invoice OWNER TO pa;

--
-- Name: invoice_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.invoice_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.invoice_id_seq OWNER TO pa;

--
-- Name: invoice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.invoice_id_seq OWNED BY public.invoice.id;


--
-- Name: loan; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.loan (
    id integer NOT NULL,
    account_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    loan_type_id integer NOT NULL,
    contract_id integer NOT NULL,
    principal integer NOT NULL,
    currency_id integer NOT NULL
);


ALTER TABLE public.loan OWNER TO pa;

--
-- Name: loan_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.loan_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.loan_id_seq OWNER TO pa;

--
-- Name: loan_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.loan_id_seq OWNED BY public.loan.id;


--
-- Name: payment_plan_item_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.payment_plan_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_plan_item_id_seq OWNER TO pa;

--
-- Name: payment_plan_item; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.payment_plan_item (
    id integer DEFAULT nextval('public.payment_plan_item_id_seq'::regclass) NOT NULL,
    payment_plan_id integer NOT NULL,
    amortization integer NOT NULL,
    interest integer NOT NULL
);


ALTER TABLE public.payment_plan_item OWNER TO pa;

--
-- Name: loan_payment_plan_item_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.loan_payment_plan_item_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.loan_payment_plan_item_id_seq OWNER TO pa;

--
-- Name: loan_payment_plan_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.loan_payment_plan_item_id_seq OWNED BY public.payment_plan_item.id;


--
-- Name: payment_structure_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.payment_structure_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_structure_id_seq OWNER TO pa;

--
-- Name: payment_structure; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.payment_structure (
    id integer DEFAULT nextval('public.payment_structure_id_seq'::regclass) NOT NULL,
    loan_id integer NOT NULL,
    type_id integer NOT NULL
);


ALTER TABLE public.payment_structure OWNER TO pa;

--
-- Name: TABLE payment_structure; Type: COMMENT; Schema: public; Owner: pa
--

COMMENT ON TABLE public.payment_structure IS 'Abstract recursive description of executing the loan obligation until finalized.';


--
-- Name: COLUMN payment_structure.loan_id; Type: COMMENT; Schema: public; Owner: pa
--

COMMENT ON COLUMN public.payment_structure.loan_id IS 'To be changed to contract_id';


--
-- Name: loan_payment_structure_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.loan_payment_structure_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.loan_payment_structure_id_seq OWNER TO pa;

--
-- Name: loan_payment_structure_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.loan_payment_structure_id_seq OWNED BY public.payment_structure.id;


--
-- Name: payment_structure_item_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.payment_structure_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_structure_item_id_seq OWNER TO pa;

--
-- Name: payment_structure_term; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.payment_structure_term (
    id integer DEFAULT nextval('public.payment_structure_item_id_seq'::regclass) NOT NULL,
    payment_structure_id integer NOT NULL,
    type_id integer NOT NULL,
    value integer,
    applies_after interval,
    applies_for interval
);


ALTER TABLE public.payment_structure_term OWNER TO pa;

--
-- Name: COLUMN payment_structure_term.type_id; Type: COMMENT; Schema: public; Owner: pa
--

COMMENT ON COLUMN public.payment_structure_term.type_id IS 'Types the value column';


--
-- Name: loan_payment_structure_item_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.loan_payment_structure_item_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.loan_payment_structure_item_id_seq OWNER TO pa;

--
-- Name: loan_payment_structure_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.loan_payment_structure_item_id_seq OWNED BY public.payment_structure_term.id;


--
-- Name: payment_structure_item_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.payment_structure_item_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_structure_item_type_id_seq OWNER TO pa;

--
-- Name: payment_structure_term_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.payment_structure_term_type (
    id integer DEFAULT nextval('public.payment_structure_item_type_id_seq'::regclass) NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.payment_structure_term_type OWNER TO pa;

--
-- Name: loan_payment_structure_item_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.loan_payment_structure_item_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.loan_payment_structure_item_type_id_seq OWNER TO pa;

--
-- Name: loan_payment_structure_item_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.loan_payment_structure_item_type_id_seq OWNED BY public.payment_structure_term_type.id;


--
-- Name: loan_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.loan_type (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.loan_type OWNER TO pa;

--
-- Name: loan_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.loan_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.loan_type_id_seq OWNER TO pa;

--
-- Name: loan_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.loan_type_id_seq OWNED BY public.loan_type.id;


--
-- Name: party_role; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.party_role (
    id integer NOT NULL,
    name text NOT NULL,
    description text DEFAULT '""'::text NOT NULL
);


ALTER TABLE public.party_role OWNER TO pa;

--
-- Name: party_role_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.party_role_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.party_role_id_seq OWNER TO pa;

--
-- Name: party_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.party_role_id_seq OWNED BY public.party_role.id;


--
-- Name: payment_plan; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.payment_plan (
    id integer NOT NULL,
    loan_id integer NOT NULL
);


ALTER TABLE public.payment_plan OWNER TO pa;

--
-- Name: TABLE payment_plan; Type: COMMENT; Schema: public; Owner: pa
--

COMMENT ON TABLE public.payment_plan IS 'The concrete plan with one item per invoicable period.';


--
-- Name: COLUMN payment_plan.loan_id; Type: COMMENT; Schema: public; Owner: pa
--

COMMENT ON COLUMN public.payment_plan.loan_id IS 'Also to be changed to contract_id, as per the idea fo payment_structure.loan_id ?';


--
-- Name: payment_plan_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.payment_plan_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_plan_id_seq OWNER TO pa;

--
-- Name: payment_plan_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.payment_plan_id_seq OWNED BY public.payment_plan.id;


--
-- Name: payment_structure_interest_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.payment_structure_interest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_structure_interest_id_seq OWNER TO pa;

--
-- Name: payment_structure_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.payment_structure_type (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.payment_structure_type OWNER TO pa;

--
-- Name: payment_structure_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.payment_structure_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.payment_structure_type_id_seq OWNER TO pa;

--
-- Name: payment_structure_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.payment_structure_type_id_seq OWNED BY public.payment_structure_type.id;


--
-- Name: product; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.product (
    id integer NOT NULL,
    name text NOT NULL,
    product_type_id integer NOT NULL
);


ALTER TABLE public.product OWNER TO pa;

--
-- Name: product_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.product_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.product_id_seq OWNER TO pa;

--
-- Name: product_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.product_id_seq OWNED BY public.product.id;


--
-- Name: product_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.product_type (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.product_type OWNER TO pa;

--
-- Name: product_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.product_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.product_type_id_seq OWNER TO pa;

--
-- Name: product_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.product_type_id_seq OWNED BY public.product_type.id;


--
-- Name: reference; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.reference (
    id integer NOT NULL,
    image text NOT NULL,
    create_date date NOT NULL
);


ALTER TABLE public.reference OWNER TO pa;

--
-- Name: reference_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.reference_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.reference_id_seq OWNER TO pa;

--
-- Name: reference_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.reference_id_seq OWNED BY public.reference.id;


--
-- Name: schedule_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.schedule_type (
    id integer NOT NULL,
    name text
);


ALTER TABLE public.schedule_type OWNER TO pa;

--
-- Name: schedule_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.schedule_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.schedule_type_id_seq OWNER TO pa;

--
-- Name: schedule_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.schedule_type_id_seq OWNED BY public.schedule_type.id;


--
-- Name: transaction; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.transaction (
    id bigint NOT NULL,
    amount integer NOT NULL,
    currency_id integer NOT NULL,
    value_date timestamp without time zone NOT NULL,
    transaction_type_id integer NOT NULL
);


ALTER TABLE public.transaction OWNER TO pa;

--
-- Name: transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.transaction_id_seq OWNER TO pa;

--
-- Name: transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.transaction_id_seq OWNED BY public.transaction.id;


--
-- Name: transaction_type; Type: TABLE; Schema: public; Owner: pa
--

CREATE TABLE public.transaction_type (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.transaction_type OWNER TO pa;

--
-- Name: transaction_type_id_seq; Type: SEQUENCE; Schema: public; Owner: pa
--

CREATE SEQUENCE public.transaction_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.transaction_type_id_seq OWNER TO pa;

--
-- Name: transaction_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: pa
--

ALTER SEQUENCE public.transaction_type_id_seq OWNED BY public.transaction_type.id;


--
-- Name: account id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account ALTER COLUMN id SET DEFAULT nextval('public.account_id_seq'::regclass);


--
-- Name: account_entry id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account_entry ALTER COLUMN id SET DEFAULT nextval('public.account_entry_id_seq'::regclass);


--
-- Name: account_party account_id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account_party ALTER COLUMN account_id SET DEFAULT nextval('public.account_party_account_id_seq'::regclass);


--
-- Name: account_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account_type ALTER COLUMN id SET DEFAULT nextval('public.account_type_id_seq'::regclass);


--
-- Name: contract id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.contract ALTER COLUMN id SET DEFAULT nextval('public.contract_id_seq'::regclass);


--
-- Name: contract_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.contract_type ALTER COLUMN id SET DEFAULT nextval('public.contract_type_id_seq'::regclass);


--
-- Name: currency id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.currency ALTER COLUMN id SET DEFAULT nextval('public.currency_id_seq'::regclass);


--
-- Name: invoice id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.invoice ALTER COLUMN id SET DEFAULT nextval('public.invoice_id_seq'::regclass);


--
-- Name: loan id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.loan ALTER COLUMN id SET DEFAULT nextval('public.loan_id_seq'::regclass);


--
-- Name: loan_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.loan_type ALTER COLUMN id SET DEFAULT nextval('public.loan_type_id_seq'::regclass);


--
-- Name: party id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.party ALTER COLUMN id SET DEFAULT nextval('public.entity_id_seq'::regclass);


--
-- Name: party_role id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.party_role ALTER COLUMN id SET DEFAULT nextval('public.party_role_id_seq'::regclass);


--
-- Name: party_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.party_type ALTER COLUMN id SET DEFAULT nextval('public.entity_type_id_seq'::regclass);


--
-- Name: payment_plan id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_plan ALTER COLUMN id SET DEFAULT nextval('public.payment_plan_id_seq'::regclass);


--
-- Name: payment_structure_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_structure_type ALTER COLUMN id SET DEFAULT nextval('public.payment_structure_type_id_seq'::regclass);


--
-- Name: product id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.product ALTER COLUMN id SET DEFAULT nextval('public.product_id_seq'::regclass);


--
-- Name: product_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.product_type ALTER COLUMN id SET DEFAULT nextval('public.product_type_id_seq'::regclass);


--
-- Name: reference id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.reference ALTER COLUMN id SET DEFAULT nextval('public.reference_id_seq'::regclass);


--
-- Name: schedule_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.schedule_type ALTER COLUMN id SET DEFAULT nextval('public.schedule_type_id_seq'::regclass);


--
-- Name: transaction id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.transaction ALTER COLUMN id SET DEFAULT nextval('public.transaction_id_seq'::regclass);


--
-- Name: transaction_type id; Type: DEFAULT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.transaction_type ALTER COLUMN id SET DEFAULT nextval('public.transaction_type_id_seq'::regclass);


--
-- Name: account_entry account_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account_entry
    ADD CONSTRAINT account_entry_pkey PRIMARY KEY (id);


--
-- Name: account_party account_party_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account_party
    ADD CONSTRAINT account_party_pkey PRIMARY KEY (account_id);


--
-- Name: account account_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);


--
-- Name: account_type account_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.account_type
    ADD CONSTRAINT account_type_pkey PRIMARY KEY (id);


--
-- Name: contract_party contract_party_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.contract_party
    ADD CONSTRAINT contract_party_pkey PRIMARY KEY (contract_id, party_role_id, party_id);


--
-- Name: contract contract_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.contract
    ADD CONSTRAINT contract_pkey PRIMARY KEY (id);


--
-- Name: contract_type contract_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.contract_type
    ADD CONSTRAINT contract_type_pkey PRIMARY KEY (id);


--
-- Name: currency currency_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.currency
    ADD CONSTRAINT currency_pkey PRIMARY KEY (id);


--
-- Name: invoice invoice_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT invoice_pkey PRIMARY KEY (id);


--
-- Name: payment_structure_term_type loan_payment_structure_item_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_structure_term_type
    ADD CONSTRAINT loan_payment_structure_item_type_pkey PRIMARY KEY (id);


--
-- Name: payment_structure loan_payment_structure_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_structure
    ADD CONSTRAINT loan_payment_structure_pkey PRIMARY KEY (id);


--
-- Name: loan loan_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.loan
    ADD CONSTRAINT loan_pkey PRIMARY KEY (id);


--
-- Name: loan_type loan_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.loan_type
    ADD CONSTRAINT loan_type_pkey PRIMARY KEY (id);


--
-- Name: party party_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.party
    ADD CONSTRAINT party_pkey PRIMARY KEY (id);


--
-- Name: party_role party_role_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.party_role
    ADD CONSTRAINT party_role_pkey PRIMARY KEY (id);


--
-- Name: party_type party_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.party_type
    ADD CONSTRAINT party_type_pkey PRIMARY KEY (id);


--
-- Name: payment_plan_item payment_plan_item_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_plan_item
    ADD CONSTRAINT payment_plan_item_pkey PRIMARY KEY (id);


--
-- Name: payment_plan payment_plan_loan_id_key; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_plan
    ADD CONSTRAINT payment_plan_loan_id_key UNIQUE (loan_id);


--
-- Name: payment_plan payment_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_plan
    ADD CONSTRAINT payment_plan_pkey PRIMARY KEY (id);


--
-- Name: payment_structure_term payment_structure_item_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_structure_term
    ADD CONSTRAINT payment_structure_item_pkey PRIMARY KEY (id);


--
-- Name: payment_structure payment_structure_loan_id_key; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_structure
    ADD CONSTRAINT payment_structure_loan_id_key UNIQUE (loan_id);


--
-- Name: payment_structure_type payment_structure_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.payment_structure_type
    ADD CONSTRAINT payment_structure_type_pkey PRIMARY KEY (id);


--
-- Name: product product_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);


--
-- Name: product_type product_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.product_type
    ADD CONSTRAINT product_type_pkey PRIMARY KEY (id);


--
-- Name: reference reference_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.reference
    ADD CONSTRAINT reference_pkey PRIMARY KEY (id);


--
-- Name: schedule_type schedule_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.schedule_type
    ADD CONSTRAINT schedule_type_pkey PRIMARY KEY (id);


--
-- Name: transaction transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_pkey PRIMARY KEY (id);


--
-- Name: transaction_type transaction_type_pkey; Type: CONSTRAINT; Schema: public; Owner: pa
--

ALTER TABLE ONLY public.transaction_type
    ADD CONSTRAINT transaction_type_pkey PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

