package com.sothawo.akkabatch;

import org.apache.commons.lang3.StringUtils;

/**
 * Datensatz.
 */
public final class Record {
// ------------------------------ FIELDS ------------------------------

    private String id;
    private String sex;
    private String lastname;
    private String firstname;
    private String zip;
    private String city;
    private String district;
    private String street;
    private String number;

// -------------------------- STATIC METHODS --------------------------

    public static Record fromLine(String line) {
        if (null == line) {
            throw new IllegalArgumentException("Record from null");
        }
        String[] fields = line.split("~", -1);
        if (fields.length != 10) {
            throw new IllegalArgumentException("ungültige Daten: " + line);
        }
        Record record = new Record();
        record.id = fields[0];
        record.sex = fields[1];
        record.firstname = fields[2];
        record.lastname = fields[3];
        record.zip = fields[4];
        record.city = fields[5];
        record.district = fields[6];
        record.street = fields[7];
        record.number = fields[8] + fields[9];
        return record;
    }

    public static Record processRecord(final Record origin) {
        if (null == origin) {
            throw new IllegalArgumentException("null input");
        }
        Record record = new Record();
        record.id = origin.id;
        record.sex = origin.sex;
        record.firstname = StringUtils.upperCase(origin.firstname);
        record.lastname = StringUtils.upperCase(origin.lastname);
        record.zip = origin.zip;
        record.city = StringUtils.upperCase(origin.city);
        record.district = StringUtils.upperCase(origin.district);
        record.street = StringUtils.upperCase(origin.street);
        record.number = StringUtils.upperCase(origin.number);
        try {
            Thread.sleep(1);
        } catch (InterruptedException ignored) {
            System.err.println("oops, interrupted");
        }
        return record;
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
