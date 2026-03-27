(function () {
    'use strict';

    const SEARCH_API = '/api/comics/search?q=';
    const SEARCH_PAGE = '/search?keyword=';
    const DEBOUNCE_MS = 280;
    const MINIO_URL = '';

    function debounce(fn, delay) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    function formatViews(n) {
        if (!n && n !== 0) return '';
        if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
        if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
        return String(n);
    }

    function formatChapterNum(num) {
        if (!num) return '';
        return num.stripTrailingZeros ? num.stripTrailingZeros().toPlainString() : num;
    }

    function getCoverUrl(coverImage) {
        if (!coverImage) return null;
        if (coverImage.startsWith('http')) return coverImage;
        return MINIO_URL + '/comics/' + coverImage;
    }

    function buildResultHTML(comics, query) {
        if (!comics || comics.length === 0) {
            return `<div class="search-result-item" style="justify-content:center;color:#94a3b8;">
                        Không tìm thấy truyện nào cho "<strong style="color:white;">${escapeHtml(query)}</strong>"
                    </div>`;
        }
        return comics.map(c => {
            const coverUrl = getCoverUrl(c.coverImage);
            const img = coverUrl
                ? `<img src="${coverUrl}" alt="${escapeHtml(c.title)}" class="search-result-thumbnail">`
                : `<div class="search-result-thumbnail" style="background:#2d1b47;display:flex;align-items:center;justify-content:center;"><i class="bi bi-book" style="color:var(--primary)"></i></div>`;
            
            const latestChapter = c.latestChapterNum 
                ? `<span class="search-result-chapter">Ch. ${formatChapterNum(c.latestChapterNum)}</span>`
                : `<span class="search-result-chapter">Chưa có chap</span>`;
            
            return `<a href="/comic-detail/${c.comicId}" class="search-result-item text-decoration-none">
                        ${img}
                        <div class="search-result-info">
                            <div class="search-result-title">${escapeHtml(c.title)}</div>
                            ${latestChapter}
                        </div>
                    </a>`;
        }).join('');
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    async function doSearch(query, resultsEl, footerEl, footerLinkEl, dropdownEl) {
        if (!query || query.trim().length < 1) {
            if (dropdownEl) dropdownEl.classList.remove('show');
            return;
        }
        try {
            const resp = await fetch(SEARCH_API + encodeURIComponent(query.trim()));
            if (!resp.ok) throw new Error('Search failed');
            const data = await resp.json();
            resultsEl.innerHTML = buildResultHTML(data, query);
            if (footerEl && footerLinkEl) {
                footerLinkEl.textContent = `Xem tất cả kết quả cho "${query.trim()}"`;
                footerLinkEl.href = SEARCH_PAGE + encodeURIComponent(query.trim());
                footerEl.style.display = '';
            }
            if (dropdownEl) dropdownEl.classList.add('show');
        } catch (e) {
            resultsEl.innerHTML = `<div class="search-result-item" style="color:#ef4444;">Lỗi tìm kiếm. Vui lòng thử lại.</div>`;
            if (dropdownEl) dropdownEl.classList.add('show');
        }
    }

    function initDesktopSearch() {
        const input = document.getElementById('searchInput');
        const dropdown = document.getElementById('searchDropdown');
        const resultsEl = document.getElementById('searchResults');
        const footerEl = document.getElementById('searchFooter');
        const footerLink = document.getElementById('searchAllLink');
        if (!input || !dropdown || !resultsEl) return;

        const debouncedSearch = debounce(q => doSearch(q, resultsEl, footerEl, footerLink, dropdown), DEBOUNCE_MS);

        input.addEventListener('input', function () {
            const q = this.value.trim();
            if (q.length === 0) {
                dropdown.classList.remove('show');
                footerEl && (footerEl.style.display = 'none');
            } else {
                debouncedSearch(q);
            }
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && this.value.trim()) {
                window.location.href = SEARCH_PAGE + encodeURIComponent(this.value.trim());
            }
        });

        document.addEventListener('click', function (e) {
            if (!e.target.closest('.search-container')) {
                dropdown.classList.remove('show');
            }
        });

        dropdown.addEventListener('click', function (e) {
            e.stopPropagation();
        });
    }

    function initMobileSearch() {
        const input = document.getElementById('mobileSearchInput');
        const resultsEl = document.getElementById('mobileSearchResults');
        const footerEl = document.getElementById('mobileSearchFooter');
        const footerLink = document.getElementById('mobileSearchAllLink');
        if (!input || !resultsEl) return;

        const debouncedSearch = debounce(q => doSearch(q, resultsEl, footerEl, footerLink, null), DEBOUNCE_MS);

        input.addEventListener('input', function () {
            const q = this.value.trim();
            if (q.length === 0) {
                resultsEl.innerHTML = '';
                footerEl && (footerEl.style.display = 'none');
            } else {
                debouncedSearch(q);
            }
        });

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && this.value.trim()) {
                window.location.href = SEARCH_PAGE + encodeURIComponent(this.value.trim());
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initDesktopSearch();
        initMobileSearch();
    });
})();

