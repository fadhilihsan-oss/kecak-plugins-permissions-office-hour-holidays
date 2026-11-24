CREATE TABLE app_holiday_calendar (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(255) NOT NULL,
    holiday_type VARCHAR(20) NOT NULL,
    year INTEGER NOT NULL,
    source VARCHAR(255),
    CONSTRAINT uq_app_holiday_calendar_date_type UNIQUE (holiday_date, holiday_type)
);
