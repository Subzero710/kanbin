document.addEventListener("DOMContentLoaded", function() {
    // 1. CONFIGURATION ROBUSTE (URL + CSRF (SI PRESENT))
    const baseUrlMeta = document.querySelector('meta[name="api-base-url"]');
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    // On s'assure que le chemin finit toujours par '/'
    let rawPath = baseUrlMeta ? baseUrlMeta.getAttribute('content') : '/';
    const CONTEXT_PATH = rawPath.endsWith('/') ? rawPath : rawPath + '/';

    const CSRF_TOKEN = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : null;
    const CSRF_HEADER = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : null;

    /**
     * Construit une URL API absolue et valide
     * @param {string} endpoint - ex: "board/move-issue"
     */
    function getApiUrl(endpoint) {
        // Retire le slash au début de l'endpoint pour éviter le double slash //
        const cleanEndpoint = endpoint.startsWith('/') ? endpoint.substring(1) : endpoint;
        return CONTEXT_PATH + cleanEndpoint;
    }

    /**
     * Wrapper pour fetch qui ajoute automatiquement le token de sécurité CSRF
     */
    function secureFetch(endpoint, formData) {
        const headers = {};
        // Ajout du header CSRF si présent (obligatoire pour POST Spring Security)
        if (CSRF_TOKEN && CSRF_HEADER) {
            headers[CSRF_HEADER] = CSRF_TOKEN;
        }

        // Utilisation URLSearchParams qui envoie du application/x-www-form-urlencoded par défaut.

        return fetch(getApiUrl(endpoint), {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                ...headers // Injection du token
            },
            body: formData
        }).then(response => {
            if (!response.ok) {
                // Si erreur 403 (Forbidden) ou 404 or 500, on lève une erreur explicite
                throw new Error(`Erreur HTTP ${response.status} sur ${endpoint}`);
            }
            // On vérifie le type de contenu avant de parser JSON
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                return response.json();
            } else {
                return response.text(); // Pour gérer les réponses "OK" brutes
            }
        });
    }

    const columns = document.querySelectorAll('.kb-main-col');
    const board = document.getElementById('board');
    let draggedItem = null;

    // ============================================================
    // WIP COUNTERS (badges "X / Y" dans l'entête des colonnes)
    // ============================================================
    function initWipBadgeLimits() {
        document.querySelectorAll('.kb-main-col').forEach(mainCol => {
            const badge = mainCol.querySelector('.kb-col-header .badge');
            if (!badge) return;

            // extrait le "Y" de "X / Y" pour le garder stable
            const m = badge.textContent.match(/(\d+)\s*\/\s*(\d+)/);
            if (m) {
                badge.dataset.limit = m[2];
            }
        });
    }

    function refreshWipCounters() {
        document.querySelectorAll('.kb-main-col').forEach(mainCol => {
            const badge = mainCol.querySelector('.kb-col-header .badge');
            if (!badge) return;
            const limitStr = badge.dataset.limit;
            if (!limitStr) return;

            const limit = parseInt(limitStr, 10);
            const usage = mainCol.querySelectorAll('.kb-col-body .issue-draggable').length;
            badge.textContent = usage + " / " + limit;
        });
    }

    initWipBadgeLimits();
    // synchronise au chargement (au cas où)
    refreshWipCounters();


    columns.forEach(col => {
        // Restriction du drag à l'en-tête
        let isCursorInHeader = false;

        // 1. ÉTAPE DE DÉTECTION (Avant le drag)
        col.addEventListener('mousedown', function(e) {
            if (!col.classList.contains('kb-col-draggable')) {
                isCursorInHeader = false;
                return;
            }
            if (e.target.closest('.kb-col-header')) {
                isCursorInHeader = true;
            } else {
                isCursorInHeader = false;
            }
        });

        // 2. DÉMARRAGE DU DRAG
        col.addEventListener('dragstart', function(e) {
            if (!col.classList.contains('kb-col-draggable') || !isCursorInHeader) {
                e.preventDefault();
                return;
            }

            draggedItem = this;
            setTimeout(() => this.style.opacity = '0.4', 0);
            e.dataTransfer.effectAllowed = 'move';
        });

        // Fin du drag (nettoyage)
        col.addEventListener('dragend', function() {
            this.style.opacity = '1';
            draggedItem = null;
            document.querySelectorAll('.kb-col').forEach(c => c.style.border = "");
        });

        // Au survol d'une zone de dépôt
        col.addEventListener('dragover', function(e) {
            if (!draggedItem) return;
            // On interdit explicitement le drop sur la colonne backlog
            if (this.dataset.col === 'backlog') return;
            e.preventDefault(); // Nécessaire pour autoriser le drop
        });

        col.addEventListener('dragenter', function(e) {
            if (!draggedItem) return;
            if (this === draggedItem || this.dataset.col === 'backlog') return;
            e.preventDefault();
            // On ne met une bordure que si ce n'est pas l'élément qu'on traine
            this.style.border = "4px dashed #666";
        });

        col.addEventListener('dragleave', function() {
            if (this.dataset.col === 'backlog') return;
            this.style.border = "";
        });

        // Le relachement (DROP)
        col.addEventListener('drop', function(e) {
            this.style.border = "";
            if (!draggedItem) return;
            e.preventDefault();

            // Sécurité : ne rien faire si on drop sur soi-même
            if (this === draggedItem) return;
            if (this.dataset.col === 'backlog') return;
            const mainCols = Array.from(board.querySelectorAll('.kb-main-col'));

            const draggedIndex = mainCols.indexOf(draggedItem);
            const targetIndex = mainCols.indexOf(this);

            if (draggedIndex === -1 || targetIndex === -1) {
                console.error("Impossible de trouver l'index : élément non reconnu comme colonne principale.");
                return;
            }

            if (targetIndex === 0) {
                alert("Impossible de placer une colonne avant la colonne de départ.");
                return;
            }

            if (draggedIndex < targetIndex) {
                this.after(draggedItem);
            } else {
                this.before(draggedItem);
            }

            saveNewOrder(draggedItem, draggedItem.getAttribute('data-id'), targetIndex);
        });
    });

    function saveNewOrder(element, columnId, newIndex) {
        const formData = new URLSearchParams();
        formData.append('columnId', columnId);
        formData.append('newIndex', newIndex);

        // UTILISATION DE secureFetch
        secureFetch('board/reorder-column', formData)
            .then(data => {
                // data peut être "OK" (texte) ou un objet
                if (data === "OK" || data.success) {
                    element.classList.add('flash-success');
                    setTimeout(() => element.classList.remove('flash-success'), 1500);
                } else {
                    console.error("Erreur logique:", data);
                    handleError(element);
                }
            })
            .catch(err => {
                console.error("ERREUR CRITIQUE DND:", err);
                handleError(element);
            });
    }

    function handleError(element) {
        // En cas d'erreur, on secoue l'élément en rouge
        element.classList.add('flash-error');
        setTimeout(() => {
            element.classList.remove('flash-error');
            alert("Erreur lors de la sauvegarde du déplacement. La page va être rechargée.");
            location.reload();
        }, 500);
    }

    // ============================================================
    // GESTION DU DRAG & DROP DES STORIES (ISSUES)
    // ============================================================

    const draggableIssues = document.querySelectorAll('.issue-draggable');
    let draggedIssue = null;
    let sourceContainer = null; // Pour stocker la colonne d'origine en cas de rollback

    // 1. Début du drag sur une story
    draggableIssues.forEach(issue => {
        issue.addEventListener('dragstart', function(e) {
            draggedIssue = this;
            sourceContainer = this.parentNode; // On mémorise le parent actuel

            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', this.getAttribute('data-id'));
            setTimeout(() => this.style.opacity = '0.5', 0);
            e.stopPropagation(); // Empêche la colonne de bouger
        });

        issue.addEventListener('dragend', function() {
            this.style.opacity = '1';
            draggedIssue = null;
            sourceContainer = null;
            document.querySelectorAll('.kb-col-body').forEach(b => b.style.background = "");
        });
    });

    // 2. Zone de dépôt (Corps des colonnes)
    const issueDropZones = document.querySelectorAll('.kb-col-body');

    issueDropZones.forEach(zone => {
        zone.addEventListener('dragover', function(e) {
            e.preventDefault();
        });

        zone.addEventListener('dragenter', function(e) {
            e.preventDefault();
            if (draggedIssue && this !== sourceContainer) {
                this.style.background = "#eef0f3";
            }
        });

        zone.addEventListener('dragleave', function() {
            this.style.background = "";
        });

        zone.addEventListener('drop', function(e) {
            this.style.background = "";
            if (!draggedIssue) return;

            e.preventDefault();
            e.stopPropagation();

            const targetCol = this.closest('[data-col]');
            if (!targetCol) return;

            // Optimistic UI : On déplace tout de suite
            if (this === sourceContainer) return; // Même colonne, rien à faire

            const issueId = draggedIssue.getAttribute('data-id');
            const targetKey = targetCol.getAttribute('data-col');

            // --- MODIFICATION ICI : Tri visuel immédiat ---
            // Si on dépose dans Closed, on met en haut (le plus récent)
            if (targetKey === 'closed') {
                this.prepend(draggedIssue);
            } else {
                this.appendChild(draggedIssue);
            }

            refreshWipCounters();

            // Sauvegarde AJAX avec gestion d'erreur et Rollback
            saveIssueMove(draggedIssue, issueId, targetKey, sourceContainer, this);
        });
    });

    function saveIssueMove(element, issueId, targetColumnKey, oldParent, newParent) {
        const formData = new URLSearchParams();
        formData.append('issueId', issueId);
        formData.append('targetColumnKey', targetColumnKey);

        secureFetch('board/move-issue-dnd', formData)
            .then(data => {
                if (data && data.success) {
                    // SUCCÈS
                    element.classList.add('flash-success');
                    setTimeout(() => element.classList.remove('flash-success'), 1500);
                } else {
                    // ECHEC (Limite atteinte)
                    console.warn("Move rejected:", data.message);

                    // 1. ROLLBACK : On remet la carte dans sa colonne d'origine
                    if (oldParent) {
                        oldParent.appendChild(element);
                    }
                    refreshWipCounters();
                    // 2. Feedback visuel rouge + Message
                    element.classList.add('flash-error');
                    setTimeout(() => element.classList.remove('flash-error'), 1000);

                    // On met un petit timeout pour laisser le temps au navigateur d'afficher le flash rouge avant
                    setTimeout(() => {
                        alert("Erreur : " + (data.message || "Limite atteinte"));
                    }, 100);
                }
            })
            .catch(err => {
                console.error("Network error:", err);
                // En cas de crash réseau, on annule aussi par sécurité
                if (oldParent) {
                    oldParent.appendChild(element);
                }
                refreshWipCounters();
                alert("Erreur technique : " + err.message);
            });
    }
});