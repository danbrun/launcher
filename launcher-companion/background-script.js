/** Represents the state of a tab. */
class TabInfo {
    constructor(id, url, title, capture) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.capture = capture;
    }
}

/** Represents a tab changing. */
class TabUpdatedEvent {
    constructor(info) {
        this.info = info;
    }
}

/** Represents a tab being closed. */
class TabRemovedEvent {
    constructor(id) {
        this.id = id;
    }
}

/** Container for tab events which can be send to the Launcher app. */
class TabEvent {

    static #captureOptions = {
        scale: 0.5
    };

    static #serverUrl = "http://localhost:47051/"
    static #requestTemplate = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    }

    /** Returns a new TabEvent for the given tab updated info. */
    static async fromUpdatedInfo(tab) {
        console.info("Creating TabUpdatedEvent", tab);

        var capture = await browser.tabs.captureTab(tab.id, this.#captureOptions);
        var info = new TabInfo(tab.id, tab.url, tab.title, capture);

        var event = new TabEvent();
        event.updated = new TabUpdatedEvent(info);
        return event;
    }

    /** Returns a new TabEvent for the given tab removed info. */
    static fromRemovedInfo(id) {
        console.info("Creating TabRemovedEvent", id);

        var event = new TabEvent();
        event.removed = new TabRemovedEvent(id);
        return event;
    }

    /** Sends the event to the server. */
    async sendEvent() {
        console.info("Sending TabEvent", this);

        return await fetch(TabEvent.#serverUrl, {
            ...TabEvent.#requestTemplate,
            body: JSON.stringify(this),
        })
    }
}

/** Extension entry point. */
async function main() {
    // Add listeners for browser events.
    browser.tabs.onUpdated.addListener(async (id, change, tab) => (await TabEvent.fromUpdatedInfo(tab)).sendEvent());
    browser.tabs.onRemoved.addListener(async (id) => TabEvent.fromRemovedInfo(id).sendEvent());

    // Send initial state.
    var tabs = await browser.tabs.query({});
    var events = await Promise.all(tabs.map(tab => TabEvent.fromUpdatedInfo(tab)));
    var promises = events.map(event => event.sendEvent());
    return await Promise.all(promises);
}

main();
