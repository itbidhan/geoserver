/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.script.ScriptManager;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

/**
 * Allows editing a specific Script
 */
@SuppressWarnings("serial")
public class ScriptEditPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.script.web");

    IModel scriptModel;

    GeoServerDialog dialog;

    public ScriptEditPage(PageParameters parameters) {
        String fileName = parameters.getString("file");
        Resource file = Resources.fromPath(fileName);
        Script script = new Script(file);
        if (!Resources.exists(file)) {
            error(new ParamResourceModel("ScriptEditPage.notFound", this, script).getString());
            doReturn(ScriptPage.class);
            return;
        }
        init(script);
    }

    public ScriptEditPage(Script script) {
        init(script);
    }

    private void init(Script script) {
        scriptModel = new ScriptDetachableModel(script);

        Form form = new Form("form", new CompoundPropertyModel(scriptModel)) {
            protected void onSubmit() {
                try {
                    saveScript();
                    doReturn(ScriptPage.class);
                } catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING, "Failed to save script", e);
                    error(e.getMessage() == null ? "Failed to save script, no error message available, see logs for details"
                            : e.getMessage());
                }
            }
        };
        add(form);

        // Name
        Label nameLabel = new Label("nameLabel", new PropertyModel(scriptModel, "name"));
        form.add(nameLabel);
        HiddenField name = new HiddenField("name", new PropertyModel(scriptModel, "name"));
        form.add(name);
        
        // Type
        Label typeLabel = new Label("typeLabel", new PropertyModel(scriptModel, "type"));
        form.add(typeLabel);
        HiddenField type = new HiddenField("type", new PropertyModel(scriptModel, "type"));
        form.add(type);
        
        // Extension
        Label extensionLabel = new Label("extensionLabel", new PropertyModel(scriptModel, "extension"));
        form.add(extensionLabel);
        HiddenField extension = new HiddenField("extension", new PropertyModel(scriptModel, "extension"));
        form.add(extension);

        // Content
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptMgr");
        String mode = scriptManager.lookupPluginEditorMode(script.getResource().file());
        CodeMirrorEditor content = new CodeMirrorEditor("contents", mode, new PropertyModel(scriptModel, "contents"));
        content.setRequired(true);
        form.add(content);

        // Dialog
        add(dialog = new GeoServerDialog("dialog"));

        // Submit and Cancel
        SubmitLink submit = new SubmitLink("submit");
        form.add(submit);
        form.setDefaultButton(submit);
        form.add(new BookmarkablePageLink("cancel", ScriptPage.class));
    }

    private void saveScript() {
        Script script = (Script) scriptModel.getObject();
        OutputStream out = script.getResource().out();        
        try {
            IOUtils.write(script.getContents(), out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
