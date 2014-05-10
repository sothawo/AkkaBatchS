package com.sothawo.akkabatch.java;

import java.io.Serializable;

/**
 * record.
 */
public final class Record implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private String id;
    private String sex;
    private String firstname;
    private String lastname;
    private String zip;
    private String city;
    private String district;
    private String street;
    private String number;

// -------------------------- STATIC METHODS --------------------------

    public static Record fromLine(String line) {
        if (null == line) {
            throw new IllegalArgumentException("record from null");
        }
        String[] fields = line.split("~", -1);
        if (fields.length != 10) {
            throw new IllegalArgumentException("invalid data: " + line);
        }
        return new Record(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7],
                   fields[8] + fields[9]);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public Record(String id, String sex, String firstname, String lastname, String zip, String city,
                  String district, String street, String number) {
        this.id = id;
        this.sex = sex;
        this.firstname = firstname;
        this.lastname = lastname;
        this.zip = zip;
        this.city = city;
        this.district = district;
        this.street = street;
        this.number = number;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getId() {
        return id;
    }

    public String getLastname() {
        return lastname;
    }

    public String getNumber() {
        return number;
    }

    public String getSex() {
        return sex;
    }

    public String getStreet() {
        return street;
    }

    public String getZip() {
        return zip;
    }
}
