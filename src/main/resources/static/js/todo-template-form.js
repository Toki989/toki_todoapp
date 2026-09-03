document.addEventListener("DOMContentLoaded", () => {
    const container = document.querySelector("[data-template-items]");
    const addButton = document.querySelector("[data-add-item]");
    if (!container || !addButton) return;

    const renumber = () => {
        const items = [...container.querySelectorAll("[data-template-item]")];
        items.forEach((item, index) => {
            item.querySelector("legend").textContent = `項目 ${index + 1}`;
            item.querySelectorAll("[name]").forEach((field) => {
                field.name = field.name.replace(/items\[\d+\]/, `items[${index}]`);
                if (field.id) field.id = field.id.replace(/items\d+/, `items${index}`);
            });
        });
        items.forEach((item) => {
            item.querySelector("[data-remove-item]").disabled = items.length === 1;
        });
    };

    addButton.addEventListener("click", () => {
        const source = container.querySelector("[data-template-item]");
        const item = source.cloneNode(true);
        item.querySelectorAll("input, select").forEach((field) => field.value = "");
        item.querySelectorAll(".error").forEach((error) => error.remove());
        container.appendChild(item);
        renumber();
    });

    container.addEventListener("click", (event) => {
        if (!event.target.matches("[data-remove-item]")) return;
        if (container.querySelectorAll("[data-template-item]").length > 1) {
            event.target.closest("[data-template-item]").remove();
            renumber();
        }
    });
    renumber();
});
