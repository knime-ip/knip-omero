package org.knime.knip.newomero.util;

import org.knime.core.node.defaultnodesettings.DialogComponentButton;

/**
 * Provides a component containing a button. The button's text can be set
 * individually and an <code>ActionListener</code> can be added, to respond to
 * action events. A model is not needed, since no input component like a text
 * field etc. is provided here. Thus no setting values will be saved.
 *
 * This is an adapted form that can be disabled
 *
 * @author Kilian Thiel, University of Konstanz
 * @author Gabriel Einsdorf, KNIME.com AG
 *
 */
public class DialogComponentJButton extends DialogComponentButton {

    public DialogComponentJButton(final String label) {
        super(label);
    }

    public void setButtonEnabled(final boolean enabled) {
        super.setEnabledComponents(enabled);

    }
}
