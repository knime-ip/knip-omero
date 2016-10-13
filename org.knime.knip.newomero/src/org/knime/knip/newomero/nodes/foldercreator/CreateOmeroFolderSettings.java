package org.knime.knip.newomero.nodes.foldercreator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The class storing the configuration for the omero connection node
 *
 * @author gabriel
 *
 */
public class CreateOmeroFolderSettings {

    enum TargetType {
        PROJECT("Project"), DATASET("DataSet");

        private static final List<String> valueNames =
                Arrays.asList(PROJECT.toString(), DATASET.toString());

        private String name;

        TargetType(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static Collection<String> valueNames() {
            return valueNames;
        }
    }

    public static final long ROOT_ID = -1;

    private CreateOmeroFolderSettings() {
        // Utility class
    }

    public static SettingsModelString createTargetTypeModel() {
        return new SettingsModelString("Folder Type", TargetType.PROJECT.toString());
    }

    public static SettingsModelString createTargetNameModel() {
        return new SettingsModelString("Name", "");
    }

    public static SettingsModelString createDescriptionModel() {
        return new SettingsModelString("Description", "");
    }

    public static SettingsModelLong createSelectedProjectModel() {
        return new SettingsModelLong("ID of enclosing Project", 0l);
    }

    public static SettingsModelString createVariableNameModel() {
        return new SettingsModelString("Name of created variable", "Created Object");
    }

    public static SettingsModelString createProjectSelectionModel() {
        return new SettingsModelString("(Internal, do not use!) Name of enclosing project", "");
    }
}
