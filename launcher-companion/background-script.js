"use strict";

/** Represents the state of a tab. */
class TabInfo {

    constructor(id, url, title, capture) {
        this.id = id;
        this.url = url;
        this.title = title;
        this.capture = capture;
    }

    static #captureOptions = {
        scale: 0.5
    };

    static async fromTab(tab) {
        console.info("Creating TabInfo", tab);

        var capture;
        try {
            capture = await browser.tabs.captureTab(tab.id, this.#captureOptions);
        } catch (error) {
            console.error("Failed to capture tab", tab, error);
        }

        return new TabInfo(tab.id, tab.url, tab.title, capture);
    }
}

/** Stores tabs in browser local data due to querying all not working. */
class TabStore {

    static async get() {
        return (await browser.storage.local.get("tabs")).tabs ?? {};
    }

    static async set(tabs) {
        await browser.storage.local.set({ tabs });
    }

    static async edit(lamdba) {
        var tabs = await this.get();
        lamdba(tabs);
        console.log("Edited tabs", tabs);
        await this.set(tabs);
    }

    static async put(tabInfo) {
        console.log("Put tab", tabInfo);

        await this.edit(tabs => {
            tabs[tabInfo.id] = tabInfo;
        });
    }

    static async delete(id) {
        console.info("Delete tab", id);

        await this.edit(tabs => {
            delete tabs[id];
        })
    }
}

/** Sends tabs to the companion server. */
class TabSender {

    static #serverUrl = "http://localhost:47051/"
    static #requestTemplate = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    }

    /** Sends the event to the server. */
    static async send() {
        var tabs = Object.values(await TabStore.get());
        console.info("Sending tabs", tabs);

        return await fetch(TabSender.#serverUrl, {
            ...TabSender.#requestTemplate,
            body: JSON.stringify(tabs),
        });
    }
}

/** Extension entry point. */
async function main() {
    // Add listeners for browser events.
    browser.tabs.onUpdated.addListener(async (id, change, tab) => {
        if (change.status == "complete") {
            await TabStore.put(await TabInfo.fromTab(tab));
            await TabSender.send();
        }
    });
    browser.tabs.onRemoved.addListener(async (id) => {
        await TabStore.delete(id);
        await TabSender.send();
    });
}

main();
