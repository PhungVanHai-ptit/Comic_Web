document.addEventListener("DOMContentLoaded", function() {
    const placeholder = document.getElementById('navbar-placeholder');
    
    if (placeholder) {
        fetch('navbar-client.html')
            .then(response => {
                if (!response.ok) throw new Error("Không tìm thấy file navbar.html");
                return response.text();
            })
            .then(data => {
                placeholder.innerHTML = data;
                initNavbarFunctions(); 
            })
            .catch(error => console.error("Lỗi nạp navbar:", error));
    }
});

function initNavbarFunctions() {
    console.log("Navbar đã sẵn sàng!");
    
    // --- PHẦN TÌM KIẾM ---
    const searchInput = document.getElementById('searchInput');
    const searchDropdown = document.getElementById('searchDropdown');
    
    if (searchInput && searchDropdown) {
        searchInput.addEventListener('input', function() {
            if (this.value.trim().length > 0) searchDropdown.classList.add('show');
            else searchDropdown.classList.remove('show');
        });

        document.addEventListener('click', (e) => {
            if (!e.target.closest('.search-container')) searchDropdown.classList.remove('show');
        });
    }

    // --- PHẦN ĐĂNG NHẬP / ĐĂNG XUẤT ---
    let isLoggedIn = true; // Giả lập trạng thái

    const userSection = document.getElementById('userSection');
    const authButtons = document.getElementById('authButtons');
    
    // Tìm các nút bấm cụ thể bằng ID (Bạn cần thêm ID này vào HTML trong navbar-client.html)
    const btnLogout = document.querySelector('.btn-logout');
    const btnLogin = document.querySelector('.btn-login');

    function updateAuthUI() {
        if (isLoggedIn) {
            userSection?.classList.replace('d-none', 'd-flex');
            authButtons?.classList.replace('d-flex', 'd-none');
        } else {
            userSection?.classList.replace('d-flex', 'd-none');
            authButtons?.classList.replace('d-none', 'd-flex');
        }
    }

    // Gán sự kiện trực tiếp thay vì dùng onclick trong HTML
    btnLogout?.addEventListener('click', function() {
        console.log('Đang đăng xuất...');
        isLoggedIn = false;
        updateAuthUI();
        alert('Đã đăng xuất!');
    });

    btnLogin?.addEventListener('click', function() {
        isLoggedIn = true;
        updateAuthUI();
        alert('Đã đăng nhập thành công!');
    });

    // Chạy lần đầu để đồng bộ giao diện
    updateAuthUI();
}