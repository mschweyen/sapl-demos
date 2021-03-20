package io.sapl.web.views.javabasedview;

import java.nio.charset.Charset;
import java.util.Random;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.sapl.vaadin.DocumentChangedEvent;
import io.sapl.vaadin.DocumentChangedListener;
import io.sapl.vaadin.Issue;
import io.sapl.vaadin.JsonEditor;
import io.sapl.vaadin.JsonEditorConfiguration;
import io.sapl.vaadin.SaplEditor;
import io.sapl.vaadin.SaplEditorConfiguration;
import io.sapl.vaadin.ValidationFinishedEvent;
import io.sapl.vaadin.ValidationFinishedListener;
import io.sapl.web.MainView;

@Route(value = "", layout = MainView.class)
@PageTitle("Java-based View")
@CssImport("./styles/views/javabasedview/javabased-view-view.css")
public class JavabasedViewView extends Div implements DocumentChangedListener, ValidationFinishedListener {

	private Button addDocumentChangedListenerButton;
	private Button removeDocumentChangedListenerButton;
	private Button addValidationChangedListenerButton;
	private Button removeValidationChangedListenerButton;
	private SaplEditor saplEditor;
	private JsonEditor jsonEditor;

	public JavabasedViewView() {
		setId("javabased-view-view");

		SaplEditorConfiguration saplConfig = new SaplEditorConfiguration();
		saplConfig.setHasLineNumbers(true);
		saplConfig.setTextUpdateDelay(500);

		saplEditor = new SaplEditor(saplConfig);
		saplEditor.addDocumentChangedListener(this);
		saplEditor.addValidationFinishedListener(this);
		add(saplEditor);
		
		addDocumentChangedListenerButton = new Button();
		addDocumentChangedListenerButton.setText("Add Change Listener (SAPL)");
		addDocumentChangedListenerButton.addClickListener(e -> {
			saplEditor.addDocumentChangedListener(this);
			addDocumentChangedListenerButton.setEnabled(false);
			removeDocumentChangedListenerButton.setEnabled(true);
		});
		addDocumentChangedListenerButton.setEnabled(false);
		add(addDocumentChangedListenerButton);

		removeDocumentChangedListenerButton = new Button();
		removeDocumentChangedListenerButton.setText("Remove Change Listener (SAPL)");
		removeDocumentChangedListenerButton.addClickListener(e -> {
			saplEditor.removeDocumentChangedListener(this);
			addDocumentChangedListenerButton.setEnabled(true);
			removeDocumentChangedListenerButton.setEnabled(false);
		});
		add(removeDocumentChangedListenerButton);
		
		addValidationChangedListenerButton = new Button();
		addValidationChangedListenerButton.setText("Add Validation Listener (SAPL)");
		addValidationChangedListenerButton.addClickListener(e -> {
			saplEditor.addValidationFinishedListener(this);
			addValidationChangedListenerButton.setEnabled(false);
			removeValidationChangedListenerButton.setEnabled(true);
		});
		addValidationChangedListenerButton.setEnabled(false);
		add(addValidationChangedListenerButton);

		removeValidationChangedListenerButton = new Button();
		removeValidationChangedListenerButton.setText("Remove Validation Listener (SAPL)");
		removeValidationChangedListenerButton.addClickListener(e -> {
			saplEditor.removeValidationFinishedListener(this);
			addValidationChangedListenerButton.setEnabled(true);
			removeValidationChangedListenerButton.setEnabled(false);
		});
		add(removeValidationChangedListenerButton);

		jsonEditor = new JsonEditor(new JsonEditorConfiguration());
		jsonEditor.addDocumentChangedListener(this);
		add(jsonEditor);
		
		Button getJsonDocumentButton = new Button();
		getJsonDocumentButton.setText("Get Document (JSON)");
		getJsonDocumentButton.addClickListener(e -> {
			String document = jsonEditor.getDocument();
			System.out.println("Get Document (JSON): " + document);
		});
		add(getJsonDocumentButton);

		Button setJsonDocumentButton = new Button();
		setJsonDocumentButton.setText("Set Document (JSON)");
		setJsonDocumentButton.addClickListener(e -> {
			String document = getJsonString();
			System.out.println("Set Document (JSON): " + document);
			jsonEditor.setDocument(document);
		});
		add(setJsonDocumentButton);

		saplEditor.setDocument("policy \"set by Vaadin View after instantiation ->\\u2588<-\" permit");
		jsonEditor.setDocument(getJsonString());
	}

	public void onDocumentChanged(DocumentChangedEvent event) {
		System.out.println("value changed: " + event.getNewValue());
	}

	public void onValidationFinished(ValidationFinishedEvent event) {
		System.out.println("validation finished");
		Issue[] issues = event.getIssues();
		System.out.println("issue count: " + issues.length);
		for (Issue issue : issues) {
			System.out.println(issue.getDescription());
		}
	}

	private String getJsonString() {
		return "[\r\n"
				+ " {\r\n"
				+ "  _id: \"post 1\",\r\n"
				+ "  \"author\": \"Bob\",\r\n"
				+ "  \"content\": \"...\",\r\n"
				+ "  \"page_views\": 5\r\n"
				+ " },\r\n"
				+ " {\r\n"
				+ "  \"_id\": \"post 2\",\r\n"
				+ "  \"author\": \"Bob\",\r\n"
				+ "  \"content\": \"...\",\r\n"
				+ "  \"page_views\": 9\r\n"
				+ " },\r\n"
				+ " {\r\n"
				+ "  \"_id\": \"post 3\",\r\n"
				+ "  \"author\": \"Bob\",\r\n"
				+ "  \"content\": \"...\",\r\n"
				+ "  \"page_views\": 8\r\n"
				+ " }\r\n"
				+ "]\r\n"
				+ "";
	}
}
