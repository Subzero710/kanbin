document.addEventListener("DOMContentLoaded", function() {
    const columns = document.querySelectorAll('.kb-col-draggable');
    const board = document.getElementById('board');
    let draggedItem = null;

    columns.forEach(col => {
        // Début du drag
        col.addEventListener('dragstart', function(e) {
            draggedItem = this;
            setTimeout(() => this.style.opacity = '0.4', 0);
            e.dataTransfer.effectAllowed = 'move';
        });

        // Fin du drag (nettoyage)
        col.addEventListener('dragend', function() {
            this.style.opacity = '1';
            draggedItem = null;
            // Retirer les indicateurs visuels s'il y en a
            document.querySelectorAll('.kb-col').forEach(c => c.style.border = "");
        });

        // Au survol d'une zone de dépôt
        col.addEventListener('dragover', function(e) {
            e.preventDefault(); // Nécessaire pour autoriser le drop
        });

        col.addEventListener('dragenter', function(e) {
            e.preventDefault();
            this.style.border = "2px dashed #000"; // Feedback visuel simple
        });

        col.addEventListener('dragleave', function() {
            this.style.border = "";
        });

        // Le relachement (DROP)
        col.addEventListener('drop', function(e) {
            this.style.border = "";
            e.preventDefault();

            // Sécurité : ne rien faire si on drop sur soi-même
            if (this === draggedItem) return;

            // Logique d'insertion dans le DOM
            // On insère "draggedItem" devant ou après "this" (la cible)
            const mainCols = Array.from(board.querySelectorAll('.kb-main-col'));

            const draggedIndex = mainCols.indexOf(draggedItem);
            const targetIndex = mainCols.indexOf(this);

            if (draggedIndex === -1 || targetIndex === -1) {
                console.error("Impossible de trouver l'index : élément non reconnu comme colonne principale.");
                return;
            }

            // Interdit de toucher à l'index 0 (La première colonne fixe)
            if (targetIndex === 0) {
                alert("Impossible de placer une colonne avant la colonne de départ.");
                return;
            }

            // Déplacement visuel (DOM)
            if (draggedIndex < targetIndex) {
                this.after(draggedItem);
            } else {
                this.before(draggedItem);
            }

            // Sauvegarde AJAX
            saveNewOrder(draggedItem.getAttribute('data-id'), targetIndex);
        });
    });

    function saveNewOrder(columnId, newIndex) {
        // On construit les données du formulaire
        const formData = new URLSearchParams();
        formData.append('columnId', columnId);
        formData.append('newIndex', newIndex);

        fetch('/board/reorder-column', {
            method: 'POST',
            body: formData,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            }
        })
            .then(response => response.text())
            .then(data => {
                if (data !== "OK") {
                    console.error("Erreur serveur:", data);
                    // En cas d'erreur, on recharge la page pour remettre l'ordre correct
                    location.reload();
                }
            })
            .catch(err => console.error(err));
    }
});