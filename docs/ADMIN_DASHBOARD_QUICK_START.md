# Admin Dashboard - Quick Start Guide

## Prerequisites
- Backend running on port 8080
- Frontend running on port 4200
- Admin user account created

## Starting the Application

### Backend:
```bash
cd backend
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

### Frontend:
```bash
cd frontend
npm start
```

## Accessing Admin Dashboard

### 1. Login as Admin
- Navigate to: `http://localhost:4200/login`
- Use admin credentials (created via migration or user management)
- Default test admin (if seeded):
  - Email: `admin@example.com`
  - Password: Check migration files

### 2. Admin Dashboard URL
After login, you'll be automatically redirected to:
- `http://localhost:4200/admin`

## Features Overview

### Dashboard (`/admin`)
**What you'll see:**
- Application statistics (Total, Pending, Approved, Rejected)
- Document statistics (Total, Pending, Verified, Rejected)
- User counts (Students, Admins)
- Quick access cards to all features

**Testing:**
1. Verify all stat counts display correctly
2. Click "Application Review" card → Should navigate to `/admin/applications`
3. Click "Document Verification" card → Should navigate to `/admin/documents`
4. Click "User Management" card → Should navigate to `/admin/users`

---

### Application Review (`/admin/applications`)

**Quick Test:**
1. **View Applications**
   - Page loads with all applications in a table
   - Should see: Application Code, Student Name, Email, Date, Status

2. **Search & Filter**
   - Enter a student name in "Student Name" field
   - Click "Search" → Table updates with filtered results
   - Click "Clear" → Shows all applications again

3. **Individual Actions**
   - Click the 3-dot menu (⋮) on any application
   - Select "Approve" → Status changes to APPROVED (green)
   - Select "Reject" → Status changes to REJECTED (red)
   - Success notification appears

4. **Bulk Operations**
   - Check the checkbox next to 2-3 applications
   - Click "Approve Selected" → All checked applications approved
   - Check different applications
   - Click "Reject Selected" → All checked applications rejected

5. **Pagination**
   - If more than 20 applications, pagination controls appear
   - Click "Next" → Loads next page
   - Change "Items per page" → Table updates

**Expected Results:**
- ✅ Filters work immediately
- ✅ Status updates reflect in table
- ✅ Bulk operations work on all selected items
- ✅ Pagination navigates correctly

---

### Document Verification (`/admin/documents`)

**Quick Test:**
1. **View Documents**
   - Page loads with all documents in a table
   - Shows: Document Name, Type, Student Name, Email, Upload Date, Status

2. **Download Document**
   - Click the download icon (⬇) on any document
   - File downloads to your computer
   - Success notification appears

3. **Filter Documents**
   - Select "PENDING" in Status filter
   - Click "Search" → Only pending documents show
   - Clear filter to see all

4. **Approve/Reject Documents**
   - Click 3-dot menu on a document
   - Select "Approve" → Status becomes APPROVED (green)
   - Try "Reject" on another → Status becomes REJECTED (red)

5. **Bulk Verification**
   - Select multiple pending documents (checkboxes)
   - Click "Approve Selected" → All approved
   - Select rejected documents
   - Click "Reject Selected" → All rejected

**Expected Results:**
- ✅ Documents download correctly
- ✅ Filters narrow results
- ✅ Status updates persist
- ✅ Bulk operations work

---

## Testing Checklist

### Basic Functionality
- [ ] Can login as admin
- [ ] Redirects to `/admin` after login
- [ ] Dashboard stats load and display
- [ ] Can navigate to Application Review
- [ ] Can navigate to Document Verification
- [ ] Can navigate to User Management

### Application Review
- [ ] Applications table loads
- [ ] Can search by application code
- [ ] Can search by student name
- [ ] Can filter by status
- [ ] Can filter by date range
- [ ] Can approve single application
- [ ] Can reject single application
- [ ] Can select multiple applications
- [ ] Bulk approve works
- [ ] Bulk reject works
- [ ] Pagination works
- [ ] Filters persist across pages

### Document Verification
- [ ] Documents table loads
- [ ] Can download documents
- [ ] Can search by student name
- [ ] Can filter by document type
- [ ] Can filter by status
- [ ] Can approve single document
- [ ] Can reject single document
- [ ] Can select multiple documents
- [ ] Bulk approve works
- [ ] Bulk reject works
- [ ] Pagination works

### Security
- [ ] Student users cannot access `/admin`
- [ ] Student users cannot access `/admin/applications`
- [ ] Student users cannot access `/admin/documents`
- [ ] Admin users cannot access `/dashboard` (student)
- [ ] Logged out users redirected to login

### UI/UX
- [ ] Stats cards are color-coded
- [ ] Status chips are color-coded
- [ ] Loading spinners appear during data fetch
- [ ] Success notifications appear after actions
- [ ] Error notifications appear on failures
- [ ] Empty states show when no data
- [ ] Responsive on mobile devices

---

## Common Test Scenarios

### Scenario 1: Approve Pending Applications
1. Navigate to Application Review
2. Filter by Status = "PENDING"
3. Select all pending applications (checkbox in header)
4. Click "Approve Selected"
5. Verify all change to APPROVED status
6. Return to Dashboard
7. Verify "Pending Applications" count decreased
8. Verify "Approved Applications" count increased

### Scenario 2: Verify Student Documents
1. Navigate to Document Verification
2. Filter by Status = "PENDING"
3. Click download icon on first document
4. Review document (opens in downloads folder)
5. If valid, click menu → Approve
6. If invalid, click menu → Reject
7. Repeat for all pending documents
8. Return to Dashboard
9. Verify document stats updated

### Scenario 3: Bulk Document Rejection
1. Navigate to Document Verification
2. Search for specific student: "John Doe"
3. Select all their documents
4. Click "Reject Selected"
5. Verify all documents now show REJECTED status
6. Clear search
7. Verify documents still rejected

---

## Troubleshooting

### Stats Not Loading
**Problem:** Dashboard shows 0 for all stats
**Solution:**
1. Check backend is running
2. Open browser DevTools (F12) → Network tab
3. Refresh page
4. Look for `/api/admin/stats` request
5. If 401/403 error → Check admin authentication
6. If 404 error → Check backend endpoint exists
7. If 500 error → Check backend logs

### Applications Not Appearing
**Problem:** Application table is empty
**Solution:**
1. Check browser console for errors
2. Verify backend has application data:
   - Use Postman: `GET http://localhost:8080/api/applications`
3. Check filters aren't too restrictive
4. Click "Clear" to reset filters

### Bulk Operations Not Working
**Problem:** Clicking bulk approve/reject does nothing
**Solution:**
1. Ensure items are selected (checkboxes checked)
2. Check browser console for errors
3. Verify backend endpoints are accessible
4. Check network tab for failed requests

### Documents Won't Download
**Problem:** Download button doesn't work
**Solution:**
1. Check browser console for errors
2. Verify document exists in backend
3. Check CORS settings if different domains
4. Try downloading from backend directly:
   - `GET http://localhost:8080/api/documents/download/{id}`

---

## Quick Reference

### URLs
| Feature | URL |
|---------|-----|
| Login | `http://localhost:4200/login` |
| Admin Dashboard | `http://localhost:4200/admin` |
| Application Review | `http://localhost:4200/admin/applications` |
| Document Verification | `http://localhost:4200/admin/documents` |
| User Management | `http://localhost:4200/admin/users` |

### API Endpoints
| Action | Method | Endpoint |
|--------|--------|----------|
| Get Stats | GET | `/api/admin/stats` |
| Search Applications | GET | `/api/applications/search` |
| Update Application | PATCH | `/api/applications/{id}/status` |
| Search Documents | GET | `/api/documents/search` |
| Update Document | PUT | `/api/documents/update-status/{id}` |
| Download Document | GET | `/api/documents/download/{id}` |

### Keyboard Shortcuts
- **Tab** - Navigate between form fields
- **Enter** - Submit search form
- **Space** - Toggle checkbox selection
- **Esc** - Close dropdowns/menus

---

## Performance Tips

1. **Use Pagination**
   - Set page size to 20 for faster loads
   - Increase to 50/100 only if needed

2. **Filter Before Bulk Operations**
   - Filter to specific status before bulk actions
   - Reduces number of API calls

3. **Clear Filters After Use**
   - Prevents confusion on subsequent searches
   - Ensures all data is visible

---

## Next Steps

After verifying basic functionality:
1. Test with real student data
2. Verify email notifications (if implemented)
3. Test edge cases (very long names, special characters)
4. Test with slow network (throttle in DevTools)
5. Test on mobile devices
6. Perform security testing
7. Load test with many concurrent admins

---

## Support

For issues or questions:
1. Check browser console for errors
2. Check backend logs for exceptions
3. Review implementation guide: `ADMIN_DASHBOARD_IMPLEMENTATION.md`
4. Verify all migrations ran successfully
5. Ensure environment variables are set correctly

---

**Status:** ✅ Admin Dashboard Ready for Testing
**Build Status:** ✅ Frontend Compiled Successfully
**Documentation:** ✅ Complete
