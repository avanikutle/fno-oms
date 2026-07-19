<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ SETTINGS SECTION ══════════════════════════════════════════════ --%>
<section class="page-section" id="page-settings">
  <div class="section-header">
    <div>
      <div class="section-title">Settings & Admin</div>
      <div class="section-subtitle">Manage system configurations and options cache</div>
    </div>
  </div>

  <div class="card mb-4">
    <div class="card-header">
      <div class="card-title">Watchlist Configuration</div>
    </div>
    <div style="padding:16px;">
      <p style="margin-bottom:12px;color:var(--text-muted);font-size:12px;">
        Define which base symbols should have their options loaded into the in-memory cache for fast searching in the Watchlist.
      </p>
      
      <div style="display:flex;gap:12px;align-items:flex-end">
        <div style="flex:1">
          <label class="form-label">Cached Base Symbols</label>
          <input type="text" id="setting-basesymbols" class="form-control" placeholder="e.g. NIFTY,BANKNIFTY,SENSEX">
        </div>
        <button class="btn btn-primary" onclick="AdminSettings.saveBaseSymbols()">Save & Reload Cache</button>
      </div>
      <div id="settings-result" style="margin-top:12px"></div>
    </div>
  </div>
</section>

<script>
const AdminSettings = {
  async loadBaseSymbols() {
    try {
      const res = await API.get('/api/settings/watchlist-basesymbols');
      if (res.success && res.data) {
        document.getElementById('setting-basesymbols').value = res.data;
      }
    } catch(e) {
      console.error(e);
    }
  },

  async saveBaseSymbols() {
    const val = document.getElementById('setting-basesymbols').value;
    const resEl = document.getElementById('settings-result');
    if (!val) {
      Toast.error('Validation', 'Enter at least one symbol');
      return;
    }
    
    try {
      const res = await API.post('/api/settings/watchlist-basesymbols', { symbols: val });
      if (res.success) {
        Toast.success('Settings Saved', 'Instrument cache is reloading...');
        resEl.innerHTML = `<span class="badge badge-green">✓ Saved</span>`;
      } else {
        Toast.error('Save Failed', res.message);
        resEl.innerHTML = `<span class="badge badge-red">✗ Failed</span>`;
      }
    } catch(e) {
      Toast.error('Error', e.message);
    }
  }
};

document.addEventListener('DOMContentLoaded', () => {
  if (typeof Settings !== 'undefined') {
    const _originalLoad = Settings.load;
    Settings.load = async function() {
      if (_originalLoad) await _originalLoad.call(this);
      await AdminSettings.loadBaseSymbols();
    };
  }
});
</script>
