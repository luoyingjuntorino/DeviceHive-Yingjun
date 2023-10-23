---
-- #%L
-- DeviceHive Dao RDBMS Implementation
-- %%
-- Copyright (C) 2016 DataArt
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
---
-- Create Iexperiment table
CREATE TABLE iexperiment (
  id             BIGSERIAL    NOT NULL,
  name           VARCHAR(128) NOT NULL,
  description    VARCHAR(128) NULL,
  entity_version BIGINT       NOT NULL DEFAULT 0
);

ALTER TABLE iexperiment ADD CONSTRAINT iexperiment_pk PRIMARY KEY (id);
ALTER TABLE iexperiment ADD CONSTRAINT iexperiment_name_unique UNIQUE (name);

-- Create User-Iexperiment table
CREATE TABLE user_iexperiment (
  id             BIGSERIAL NOT NULL,
  user_id        BIGINT    NOT NULL,
  iexperiment_id     BIGINT    NOT NULL,
  entity_version BIGINT DEFAULT 0
);

ALTER TABLE user_iexperiment ADD CONSTRAINT user_iexperiment_pk PRIMARY KEY (id);
ALTER TABLE user_iexperiment ADD CONSTRAINT user_iexperiment_user_pk FOREIGN KEY (user_id) REFERENCES dh_user (id);
ALTER TABLE user_iexperiment ADD CONSTRAINT user_iexperiment_iexperiment_pk FOREIGN KEY (iexperiment_id) REFERENCES iexperiment (id);

-- Add iexperiment column for device table
ALTER TABLE device ADD COLUMN iexperiment_id BIGINT NULL;
ALTER TABLE device ADD CONSTRAINT device_iexperiment_fk FOREIGN KEY (iexperiment_id) REFERENCES iexperiment (id) ON DELETE CASCADE;
CREATE INDEX device_iexperiment_id_idx ON device(iexperiment_id);

-- Default iexperiment
INSERT INTO iexperiment (name, description)
  VALUES
  ('Default Iexperiment', 'Default DeviceHive iexperiment');
INSERT INTO user_iexperiment (user_id, iexperiment_id) VALUES (1, 1);
UPDATE device SET iexperiment_id = 1 WHERE device_id = 'e50d6085-2aba-48e9-b1c3-73c673e414be';


-- -- Create Icomponent table
-- CREATE TABLE icomponent (
--   id             BIGSERIAL    NOT NULL,
--   name           VARCHAR(128) NOT NULL,
--   description    VARCHAR(128) NULL,
--   entity_version BIGINT       NOT NULL DEFAULT 0
-- );

-- ALTER TABLE icomponent ADD CONSTRAINT icomponent_pk PRIMARY KEY (id);
-- ALTER TABLE icomponent ADD CONSTRAINT icomponent_name_unique UNIQUE (name);

-- -- Create User-Icomponent table
-- CREATE TABLE user_icomponent (
--   id             BIGSERIAL NOT NULL,
--   user_id        BIGINT    NOT NULL,
--   icomponent_id     BIGINT    NOT NULL,
--   entity_version BIGINT DEFAULT 0
-- );

-- ALTER TABLE user_icomponent ADD CONSTRAINT user_icomponent_pk PRIMARY KEY (id);
-- ALTER TABLE user_icomponent ADD CONSTRAINT user_icomponent_user_pk FOREIGN KEY (user_id) REFERENCES dh_user (id);
-- ALTER TABLE user_icomponent ADD CONSTRAINT user_icomponent_icomponent_pk FOREIGN KEY (icomponent_id) REFERENCES icomponent (id);

-- -- Add icomponent column for device table
-- ALTER TABLE device ADD COLUMN icomponent_id BIGINT NULL;
-- ALTER TABLE device ADD CONSTRAINT device_icomponent_fk FOREIGN KEY (icomponent_id) REFERENCES icomponent (id) ON DELETE CASCADE;
-- CREATE INDEX device_icomponent_id_idx ON device(icomponent_id);

-- -- Default icomponent
-- INSERT INTO icomponent (name, description)
--   VALUES
--   ('Default Icomponent', 'Default DeviceHive icomponent');
-- INSERT INTO user_icomponent (user_id, icomponent_id) VALUES (1, 1);
-- UPDATE device SET icomponent_id = 1 WHERE device_id = 'e50d6085-2aba-48e9-b1c3-73c673e414be';


-- -- Add allIcomponentsAvailable column to users
-- ALTER TABLE dh_user ADD COLUMN all_icomponents_available BOOLEAN DEFAULT TRUE;

-- -- Set allIcomponentsAvailable to true if it is null
-- UPDATE dh_user SET all_icomponents_available = TRUE WHERE all_icomponents_available IS NULL;
