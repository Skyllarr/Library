/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  skylar
 * Created: Mar 29, 2016
 */

CREATE TABLE "BOOK" (
    "ID" BIGINT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "AUTHOR" VARCHAR(255),
    "TITLE" VARCHAR(255) NOT NULL,
    "YEAROFPUBLICATION" INTEGER NOT NULL
);

CREATE TABLE "CLIENT" (
    "ID" BIGINT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "NAME" VARCHAR(255),
    "SURNAME" VARCHAR(255)
);