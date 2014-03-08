package com.sothawo.akkabatch.serial;

/**
 * Processor, der seriell arbeitet.
 *
 * @author P.J.Meisch (pj.meisch@sothawo.com)
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
