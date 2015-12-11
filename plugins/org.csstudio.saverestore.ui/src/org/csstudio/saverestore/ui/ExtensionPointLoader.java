package org.csstudio.saverestore.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.saverestore.SaveRestoreService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.ui.PartInitException;

/**
 *
 * <code>ExtensionPointLoader</code> is the utility class that loads the {@link ValueImporter}s and
 * {@link ReadbackProvider}.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public final class ExtensionPointLoader {

    private List<ValueImporterWrapper> importers;
    private Optional<ReadbackProvider> readbackProvider;

    private static final ExtensionPointLoader INSTANCE = new ExtensionPointLoader();

    /**
     * Returns the singleton instance of this class.
     *
     * @return the singleton instance
     */
    public static ExtensionPointLoader getInstance() {
        return INSTANCE;
    }

    private ExtensionPointLoader() {
    }

    /**
     * Returns an unmodifiable the list of all registered value importers.
     *
     * @return returns the list of all value importers
     */
    public synchronized List<ValueImporterWrapper> getValueImporters() {
        if (importers == null) {
            importers = new ArrayList<>();
            try {
                IExtensionRegistry extReg = org.eclipse.core.runtime.Platform.getExtensionRegistry();
                IConfigurationElement[] confElements = extReg.getConfigurationElementsFor(ValueImporter.EXT_POINT);
                for (IConfigurationElement element : confElements) {
                    ValueImporter importer = (ValueImporter) element.createExecutableExtension("importer");
                    String name = (String) element.getAttribute("name");
                    importers.add(new ValueImporterWrapper(importer, name));
                }
            } catch (CoreException e) {
                SaveRestoreService.LOGGER.log(Level.SEVERE, "Save and restore value importers could not be loaded.", e);
            }
            importers = Collections.unmodifiableList(importers);
        }
        return importers;
    }

    /**
     * Returns the registered readback provider if it exists.
     *
     * @return returns readback provider
     */
    public synchronized Optional<ReadbackProvider> getReadbackProvider() {
        if (readbackProvider == null) {
            ReadbackProvider finder = null;
            try {
                IExtensionRegistry extReg = org.eclipse.core.runtime.Platform.getExtensionRegistry();
                IConfigurationElement[] confElements = extReg.getConfigurationElementsFor(ReadbackProvider.EXT_POINT);
                if (confElements.length > 1) {
                    throw new PartInitException(
                        "Cannot instantiate readback provider. Only one provider can be defined, but "
                            + confElements.length + " were found.");
                }
                for (IConfigurationElement element : confElements) {
                    finder = (ReadbackProvider) element.createExecutableExtension("readbackprovider");
                }
            } catch (CoreException e) {
                SaveRestoreService.LOGGER.log(Level.SEVERE, "Save and restore readback provider could not be loaded.",
                    e);
            }
            readbackProvider = Optional.ofNullable(finder);
        }
        return readbackProvider;
    }

}
