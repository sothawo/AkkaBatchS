/**
 * $Id: x $
 *
 * Copyright (c) 2014 Jaroso GmbH
 *
 * http://www.jaroso.de
 *
 */
package de.jaroso.test.batchakka;

/**
 * Processor, der seriell arbeitet.
 *
 * @author P.J.Meisch (pj.meisch@jaroso.de)
 */
public class SerialProcessor extends Processor {
    /**
     * Konstruktor mit OutputWriter, setzt das field writer
     *
     * @param writer
     */
    public SerialProcessor(OutputWriter writer) {
        super(writer);
    }

    @Override
    public void process(String line) {
        Record record = Record.fromLine(line);
        // TODO: Verarbeiten
        writer.write(record.getOriginal());
    }
}
