package org.csstudio.saverestore.ui;

/**
 *
 * <code>ValueImporterWrapper</code> is a wrapper around the {@link ValueImporter} which provides an instance of the
 * implementor and the name of the importer.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ValueImporterWrapper {

    /** The name of the importer as defined in the extension point */
    public final String name;
    /** The importer */
    public final ValueImporter importer;

    /**
     * Constructs a new wrapper.
     *
     * @param importer instance of the implementor
     * @param name the name of the importer
     */
    ValueImporterWrapper(ValueImporter importer, String name) {
        this.name = name;
        this.importer = importer;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }
}
