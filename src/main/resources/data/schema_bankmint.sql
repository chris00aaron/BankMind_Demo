CREATE TABLE module ( 
    id_module smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    cod_module VARCHAR(100) UNIQUE, 
    name VARCHAR(150)
); 

CREATE TABLE role ( 
	-- Datos encriptados (usar pgcrypto en PostgreSQL)
	id_role smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    cod_role VARCHAR(100) UNIQUE, 
    name VARCHAR(150) 
); 

CREATE TABLE role_details (
   id_role_detail BIGINT ALWAYS AS IDENTITY PRIMARY KEY,
   id_role SMALLINT NOT NULL,
   id_module SMALLINT NOT NULL,
   CONSTRAINT uq_role_module UNIQUE (id_role, id_module),
   CONSTRAINT fk_detail_module FOREIGN KEY (id_module) REFERENCES module(id_module) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_detail_role  FOREIGN KEY (id_role) REFERENCES role(id_role) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE public.user ( 
	-- Datos encriptados (usar pgcrypto en PostgreSQL)
	id_user BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    dni VARCHAR(100) NOT null UNIQUE,  -- Encriptado con AES-256
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT null UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(9),
    rol smallint NOT NULL,
    
    enable BOOLEAN NOT null DEFAULT true ,
    
    -- Auditoría mejorada
	created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_access TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_user_role FOREIGN KEY (rol)  REFERENCES role(id_role) ON DELETE CASCADE ON UPDATE CASCADE
); 


-- ============================================
-- ESQUEMA DE PROYECTO BANKMIND
-- Sistema Bancario con IA
-- PostgreSQL 17
-- Fecha Ultima Actualizacion: 16-01-2026
-- Version: 02
-- ============================================


-- ============================================= 
-- TABLAS MAESTRAS (NIVEL 0)
-- ============================================= 

-- Tabla: gender 
CREATE TABLE gender ( 
    id_gender INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    gender_description VARCHAR(100) NOT NULL
); 

-- Tabla: education 
CREATE TABLE education ( 
    id_education INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    educ_description VARCHAR(100) NOT NULL
); 

-- Tabla: marriage 
CREATE TABLE marriage ( 
    id_marriage INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    marri_description VARCHAR(100) NOT NULL
); 

-- Tabla: country
CREATE TABLE country (
    id_country INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    country_description VARCHAR(100) NOT NULL
);

-- Tabla: location_type (Antes: tipo_localization)
CREATE TABLE location_type ( 
    id_location_type INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    description VARCHAR(50) NOT NULL
); 

-- Tabla: horizons_time 
CREATE TABLE horizons_time ( 
    id_horizon_time INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    label VARCHAR(20), 
    description VARCHAR(50) 
); 

-- Tabla: date_time 
CREATE TABLE date_time ( 
    id_date DATE NOT NULL PRIMARY KEY, 
    day_of_the_week smallint NOT NULL,
    day_of_month smallint NOT NULL,
    month smallint NOT NULL,
    is_holiday BOOLEAN DEFAULT FALSE
); 

-- Tabla : Clima
CREATE TABLE clima ( 
    id_clima smallint NOT NULL PRIMARY KEY, 
    descripcion VARCHAR(50) NOT NULL,
    impacto smallint NOT NULL
);

-- Tabla: localization (Relacionada con country)
CREATE TABLE localization ( 
    id_localization BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_country INT,
    customer_lat DOUBLE PRECISION, 
    customer_long DOUBLE PRECISION, 
    city VARCHAR(100), 
    state VARCHAR(50), 
    zip_code VARCHAR(20), 
    city_pop INT,
    CONSTRAINT fk_localization_country FOREIGN KEY (id_country) REFERENCES country(id_country)
); 

-- ============================================= 
-- TABLAS CON DEPENDENCIAS NIVEL 1 
-- ============================================= 

-- Tabla: customer
CREATE TABLE customer ( 
    id_customer BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_gender INT, 
    id_education INT, 
    id_marriage INT, 
    id_country INT,
    id_localization BIGINT,
    id_registration_date timestamp, 
    surname VARCHAR(100), 
    first_name VARCHAR(100), 
    dob DATE, 
    age INT, 
    job VARCHAR(150), 
    CONSTRAINT fk_customer_gender FOREIGN KEY (id_gender) REFERENCES gender(id_gender), 
    CONSTRAINT fk_customer_education FOREIGN KEY (id_education) REFERENCES education(id_education), 
    CONSTRAINT fk_customer_marriage FOREIGN KEY (id_marriage) REFERENCES marriage(id_marriage), 
    CONSTRAINT fk_customer_country FOREIGN KEY (id_country) REFERENCES country(id_country),
    CONSTRAINT fk_customer_localization FOREIGN KEY (id_localization) REFERENCES localization(id_localization)
); 

-- Tabla: atms 
CREATE TABLE atms ( 
    id_atm BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_location_type INT, 
    max_capacity DECIMAL(15,3), 
    CONSTRAINT fk_atms_location_type FOREIGN KEY (id_location_type) REFERENCES location_type(id_location_type) 
); 

-- ============================================= 
-- TABLAS CON DEPENDENCIAS NIVEL 2 
-- ============================================= 

-- Tabla: account_details
CREATE TABLE account_details ( 
    record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_customer BIGINT, 
    limit_bal DECIMAL(15,2), 
    num_of_products INT, 
    balance DECIMAL(15,2), 
    has_cr_card BOOLEAN, 
    estimated_salary DECIMAL(15,2), 
    tenure INT, 
    credit_score INT, 
    exited BOOLEAN, 
    is_active_member BOOLEAN, 
    CONSTRAINT fk_accountdetails_customer FOREIGN KEY (id_customer) REFERENCES customer(id_customer) 
); 

-- Tabla: monthly_history
CREATE TABLE monthly_history ( 
    id_historial BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    record_id BIGINT, 
    monthly_period DATE, 
    pay_x INT, 
    bill_amt_X DECIMAL(15,2), 
    pay_amt_X DECIMAL(15,2), 
    did_customer_pay bit,
    expiration_date DATE,
    CONSTRAINT fk_monthly_history_account_details FOREIGN KEY (record_id) REFERENCES account_details(record_id) 
); 

-- Tabla: default_prediction
CREATE TABLE default_prediction ( 
    id_prediction BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_historial BIGINT, 
    date_prediction TIMESTAMP, 
    default_payment_next_month BOOLEAN, 
    default_probability DECIMAL(5,4), 
    main_risk_factor VARCHAR(255), 
    model_version VARCHAR(50), 
    estimated_loss DECIMAL(15,2),
    CONSTRAINT fk_default_prediction_monthly_history FOREIGN KEY (id_historial) REFERENCES monthly_history(id_historial) 
); 

-- Tabla: credit_cards
CREATE TABLE credit_cards ( 
    cc_num BIGINT PRIMARY KEY, 
    id_customer BIGINT, 
    CONSTRAINT fk_credit_cards_customer FOREIGN KEY (id_customer) REFERENCES customer(id_customer) 
); 

-- Tabla: operations_atms
CREATE TABLE operations_atms ( 
    id_operation_atms BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_atm BIGINT, 
    date_operation DATE,
    amount_withdrawn DECIMAL(15,3), 
    amount_deposited DECIMAL(15,3), 
    balance DECIMAL(15,3), 
    id_clima smallint,
    CONSTRAINT fk_operaciones_atms FOREIGN KEY (id_atm) REFERENCES atms(id_atm),
    CONSTRAINT fk_date FOREIGN KEY (date_operation) REFERENCES date_time(id_date),
    CONSTRAINT fk_clima FOREIGN KEY (id_clima) REFERENCES clima(id_clima)
); 

-- Tabla: churn predictions (Fuga Prediccion)
CREATE TABLE churn_predictions (
    id_churn_prediction BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,    
    id_customer BIGINT NOT NULL,                 
    prediction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
    churn_probability NUMERIC(15, 4),          
    is_churn BOOLEAN,                          
    risk_level VARCHAR(50),                    
    model_version VARCHAR(50) DEFAULT 'v1',    
    CONSTRAINT fk_churn_customer FOREIGN KEY (id_customer) REFERENCES customer(id_customer)
);

-- ============================================= 
-- TABLAS CON DEPENDENCIAS NIVEL 3 
-- ============================================= 

-- Tabla: operational_transactions 
CREATE TABLE operational_transactions ( 
    id_transaction BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    cc_num BIGINT, 
    trans_num VARCHAR(100) UNIQUE, 
    trans_date_time TIMESTAMP, 
    amt DECIMAL(15,2), 
    merchant VARCHAR(150), 
    category VARCHAR(100), 
    merch_lat DOUBLE PRECISION, 
    merch_long DOUBLE PRECISION, 
    unix_time BIGINT, 
    is_fraud_ground_truth INT, 
    CONSTRAINT fk_operational_transactions_credit_cards FOREIGN KEY (cc_num) REFERENCES credit_cards(cc_num) 
); 

-- Tabla: prediction_atms
CREATE TABLE prediction_atms ( 
    id_prediction_atms BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_atm BIGINT, 
    id_horizon_time INT, 
    date_prediction TIMESTAMP, 
    objective VARCHAR(50), 
    value DECIMAL(15,3), 
    CONSTRAINT fk_predictions_atms FOREIGN KEY (id_atm) REFERENCES atms(id_atm), 
    CONSTRAINT fk_predictions_horizons_time FOREIGN KEY (id_horizon_time) REFERENCES horizons_time(id_horizon_time) 
); 


-- ============================================= 
-- TABLAS CON DEPENDENCIAS NIVEL 4 
-- ============================================= 

-- Tabla: fraud_predictions 
CREATE TABLE fraud_predictions ( 
    id_fraud_prediction BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    id_transaction BIGINT, 
    xgboost_score FLOAT, 
    ifforest_score FLOAT, 
    final_decision INT, 
    detection_scenario INT, 
    prediction_date TIMESTAMP, 
    CONSTRAINT fk_fraud_predictions_transactions FOREIGN KEY (id_transaction) REFERENCES operational_transactions(id_transaction) 
);

-- ============================================= 
-- TABLAS CON DEPENDENCIAS NIVEL 5 
-- ============================================= 

CREATE TABLE prediction_details (
    id_detail BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_fraud_prediction BIGINT REFERENCES fraud_predictions(id_fraud_prediction) ON DELETE CASCADE,
    risk_factor VARCHAR(100),
    risk_points VARCHAR(10),
    risk_description TEXT,    
    risk_level VARCHAR(20)  
);