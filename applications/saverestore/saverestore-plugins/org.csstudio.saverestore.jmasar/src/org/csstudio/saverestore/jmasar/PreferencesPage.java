/*
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.csstudio.saverestore.jmasar;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public PreferencesPage() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(Activator.getInstance().getPreferenceStore());
		setMessage("JMasar configuration");
	}

	@Override
	public void init(IWorkbench arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();

        StringFieldEditor url = new StringFieldEditor(Activator.PREF_URL, "JMasar service URL:", parent) {
            @Override
            protected boolean doCheckState() {
                String txt = getTextControl().getText();
                if (txt.isEmpty()) {
                	return true;
                }
                try {
                    URL url = new URL(txt);
                    return !url.getHost().isEmpty();
                } catch (MalformedURLException e) {
                    return true;
                }
            }
        };
        url.setEmptyStringAllowed(false);
        addField(url);
	}

}
