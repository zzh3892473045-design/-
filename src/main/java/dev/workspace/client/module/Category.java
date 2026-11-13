package dev.workspace.client.module;

/**
 * Module groups shown in the ClickGUI. Combat and exploit categories are
 * intentionally absent and must not be added.
 */
public enum Category {
	RENDER("Render"),
	PLAYER("Player"),
	MISC("Misc"),
	CLIENT("Client");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return this.displayName;
	}
}
