  
        const searchInput = document.getElementById('searchInput');
        const searchDropdown = document.getElementById('searchDropdown');
        
        // Show dropdown when typing
        searchInput.addEventListener('input', function() {
            if (this.value.trim().length > 0) {
                searchDropdown.classList.add('show');
            } else {
                searchDropdown.classList.remove('show');
            }
        });
        
        // Show dropdown on focus if there's text
        searchInput.addEventListener('focus', function() {
            if (this.value.trim().length > 0) {
                searchDropdown.classList.add('show');
            }
        });
        
        // Hide dropdown when clicking outside
        document.addEventListener('click', function(event) {
            if (!event.target.closest('.search-container')) {
                searchDropdown.classList.remove('show');
            }
        });
        
        // Prevent dropdown from closing when clicking inside it
        searchDropdown.addEventListener('click', function(event) {
            event.stopPropagation();
        });
    
    
    
    
        // State management for demo
        let isLoggedIn = true; // Start with logged in state
        
        // Handle logout
        function handleLogout() {
            console.log('Logging out...');
            
            // Toggle to logged out state
            isLoggedIn = false;
            updateAuthUI();
            
            // Show alert
            setTimeout(() => {
                alert('Đã đăng xuất! Click "Log In" để đăng nhập lại.');
            }, 100);
        }
        
        // Handle showing login form (simulate login)
        function showLoginForm() {
            console.log('Logging in...');
            
            // Toggle to logged in state
            isLoggedIn = true;
            updateAuthUI();
            
            // Show alert
            setTimeout(() => {
                alert('Đã đăng nhập thành công!');
            }, 100);
        }
        
        // Cap nhat giao dien thanh menu
        function updateAuthUI() {
            const userSection = document.getElementById('userSection');
            const authButtons = document.getElementById('authButtons');
            
            if (isLoggedIn) {
                // da dang nhap
                userSection.classList.remove('d-none');
                userSection.classList.add('d-flex');
                authButtons.classList.remove('d-flex');
                authButtons.classList.add('d-none');
            } else {
                // chua dang nhap
                userSection.classList.remove('d-flex');
                userSection.classList.add('d-none');
                authButtons.classList.remove('d-none');
                authButtons.classList.add('d-flex');
            }
        }
        
        // Initialize UI on page load
        updateAuthUI();
 