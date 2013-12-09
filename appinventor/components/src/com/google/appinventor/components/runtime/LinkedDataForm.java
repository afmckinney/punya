package com.google.appinventor.components.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.SemanticWebConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AlignmentUtil;
import com.google.appinventor.components.runtime.util.RdfUtil;
import com.google.appinventor.components.runtime.util.ViewUtil;

import android.app.Activity;
import android.util.Log;
import android.view.View;

/**
 * Linked Data Form provides a layout in which contained form elements will be
 * used to generate structured data. This form is used in conjunction with
 * the SemanticWeb and LinkedData components.
 * 
 * @see SemanticWeb
 * @see LinkedDataStore
 * @author Evan W. Patton <ewpatton@gmail.com>
 *
 */
@DesignerComponent(version = YaVersion.LINKED_DATA_FORM_COMPONENT_VERSION,
    description = "A layout that provides linked data enhancement of captured data.",
    category = ComponentCategory.SEMANTICWEB)
@UsesLibraries(libraries = "xercesImpl.jar," + 
    "slf4j-android.jar," + "jena-iri.jar," + "jena-core.jar," +
    "jena-arq.jar")
@SimpleObject
public class LinkedDataForm extends AndroidViewComponent implements Component,
    ComponentContainer {

  private static final String LOG_TAG = LinkedDataForm.class.getSimpleName();
  /**
   * Stores a reference to the parent activity.
   */
  private final Activity context;

  /**
   * Linear layout used for arranging the contents of this form.
   */
  private final LinearLayout layout;

  private List<AndroidViewComponent> components;

  /**
   * String storing the URI of the concept used to type instances created with this form.
   */
  private String concept;

  /**
   * Stores the base URI used for naming new resources generated by this form.
   */
  private String baseUri;

  private String property;

  private String subject;

  private boolean inverse;

  /**
   * Creates a new linked data form in the specified container.
   * @param container
   */
  public LinkedDataForm(ComponentContainer container) {
    super(container);
    context = container.$context();
    layout = new LinearLayout(context,
        ComponentConstants.LAYOUT_ORIENTATION_VERTICAL,
        ComponentConstants.EMPTY_HV_ARRANGEMENT_WIDTH,
        ComponentConstants.EMPTY_HV_ARRANGEMENT_HEIGHT);
    AlignmentUtil alignmentSetter = new AlignmentUtil(layout);
    alignmentSetter.setHorizontalAlignment(ComponentConstants.HORIZONTAL_ALIGNMENT_DEFAULT);
    alignmentSetter.setVerticalAlignment(ComponentConstants.VERTICAL_ALIGNMENT_DEFAULT);
    components = new ArrayList<AndroidViewComponent>();
    concept = "";
    baseUri = "";
    property = "";
    Log.d(LOG_TAG, "Created linked data form");

    container.$add(this);
  }

  @Override
  public Activity $context() {
    return context;
  }

  @Override
  public Form $form() {
    return container.$form();
  }

  @Override
  public void $add(AndroidViewComponent component) {
    Log.d(LOG_TAG, "Added component to view layout");
    layout.add(component);
    components.add(component);
  }

  @Override
  public void setChildWidth(AndroidViewComponent component, int width) {
    ViewUtil.setChildWidthForVerticalLayout(component.getView(), width);
  }

  @Override
  public void setChildHeight(AndroidViewComponent component, int height) {
    ViewUtil.setChildHeightForVerticalLayout(component.getView(), height);
  }

  @Override
  public View getView() {
    Log.d(LOG_TAG, "Getting layout manager");
    return layout.getLayoutManager();
  }

  /**
   * Sets the concept URI to type objects encoded by this form.
   * @param uri
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CONCEPT_URI,
      defaultValue = "")
  @SimpleProperty
  public void ObjectType(String uri) {
    concept = uri;
  }

  /**
   * Returns the concept URI for this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Specifies the class of objects created on the Semantic Web using the contents of this form.")
  public String ObjectType() {
    return concept;
  }

  /**
   * Sets the Base URI used for generating new subject identifiers
   * @param uri URI ending in # or /
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BASEURI_AUTOGEN,
      defaultValue = SemanticWebConstants.DEFAULT_BASE_URI)
  @SimpleProperty
  public void FormID(String uri) {
    baseUri = uri;
  }

  /**
   * Gets the Base URI of this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Specifies a base URI used for constructing identifiers of objects created by this form.")
  public String FormID() {
    return baseUri;
  }

  /**
   * Sets the property URI to link a parent form to this form.
   * @param uri 
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_PROPERTY_URI,
      defaultValue = "")
  @SimpleProperty
  public void PropertyURI(String uri) {
    property = uri;
  }

  /**
   * Gets the Property URI for linking a parent form to this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Specifies the property used to link a parent form to the item generated by this form.")
  public String PropertyURI() {
    return property;
  }

  /**
   * Sets a Subject URI this form describes.
   * @param uri
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_URI,
      defaultValue = "")
  public void Subject(String uri) {
    subject = uri;
  }

  /**
   * Gets the Subject URI for this form.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Used to assert all triples in this form on a specific URI rather than autogenerate a URI.")
  public String Subject() {
    return subject;
  }

  /**
   * Sets if this form's property should be made the subject of a triple and its container the object.
   * @param inverse
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty
  public void InverseProperty(boolean inverse) {
    this.inverse = inverse;
  }

  /**
   * Gets whether or not this form represents an inverse property.
   * @return
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Reverses the direction of the generated statement.")
  public boolean InverseProperty() {
    return inverse;
  }

  @Override
  public Iterator<AndroidViewComponent> iterator() {
    Log.v(LOG_TAG, "Getting iterator for Linked Data Form. size = "+components.size());
    return components.iterator();
  }

  /**
   * Returns a URI for the form either by examining its Subject property or
   * generated from its contents.
   * @return The empty string if no valid URL can be constructed for the form,
   * or a valid URI that can be used to represent the contents of the form.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "Provides a URI for the form even if SubjectURI is not set.")
  public String GenerateSubjectURI() {
    if(Subject().length() == 0) {
      String subj = RdfUtil.generateSubjectForForm(this);
      if(subj == null) {
        return "";
      } else {
        return subj;
      }
    } else {
      return Subject();
    }
  }
}
