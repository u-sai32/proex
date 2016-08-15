// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.wizards;

import com.google.appinventor.client.Ode;
import com.google.appinventor.shared.rpc.ServerLayout;
import com.google.appinventor.shared.rpc.project.FileNode;
import com.google.appinventor.shared.rpc.project.FolderNode;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.ArrayList;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * Wizard for uploading multiple files
 */
public class MultipleFileUploadWizard extends Wizard{

  /**
   * Interface for callback to execute after files are uploaded
   */
  public static interface MultipleFileUploadedCallback {
    /**
     * Will be invoked after files are uploaded
     * @param folderNode the upload destination folder
     * @param fileNodes list of files just uploaded
     */
  void onFilesUploaded(FolderNode folderNode, ArrayList<FileNode> fileNodes);
  }

  public MultipleFileUploadWizard(FolderNode folderNode) {
    this(folderNode, null);
  }

  public MultipleFileUploadWizard(final FolderNode folderNode,
                                  final MultipleFileUploadedCallback multipleFileUploadedCallback) {
    super(MESSAGES.fileUploadWizardCaption(), true, false);

    // Initialize UI
    final FileUpload upload = new FileUpload();
    upload.setName(ServerLayout.UPLOAD_FILE_FORM_ELEMENT);
    upload.getElement().setAttribute("multiple", "multiple"); // for multiple files
    setStylePrimaryName("ode-DialogBox");
    VerticalPanel panel = new VerticalPanel();
    panel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
    panel.add(upload);
    addPage(panel);

  }

}
