/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package com.sothawo.akkabatch.serial;

/**
 * Datensatz.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class Record {
// ------------------------------ FIELDS ------------------------------

    private String original;
    private String id;
    private String sex;
    private String lastname;
    private String firstname;
    private String zip;
    private String city;
    private String district;
    private String street;
    private String number;

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public static Record fromLine(String line) {
        if(null == line) {
            throw new IllegalArgumentException("Record from null");
        }
        String[] fields = line.split("~", -1);
        if(fields.length != 10) {
            throw new IllegalArgumentException("ungültige Daten: " + line);
        }
        Record record = new Record();
        record.setOriginal(line);
        record.setId(fields[0]);
        record.setSex(fields[1]);
        record.setFirstname(fields[2]);
        record.setLastname(fields[3]);
        record.setZip(fields[4]);
        record.setCity(fields[5]);
        record.setDistrict(fields[6]);
        record.setStreet(fields[7]);
        record.setNumber(fields[8]+ fields[9]);
        return record;
    }
}
