// ============================================================
// 项目报告报表 - 使用已验证的数据获取方式
// 6大版块：资料库/项目动态/监理日志/日志(安全)/旁站/隐患台账
// ============================================================

var FU = {
  project: 'FORM-6578CA890A0D42AEA1E3DFE3806A7C27K0BZ',
  docLib: 'FORM-4ADCB90E94A44F22BC14265B222AE6E54P9S',
  dynamic: 'FORM-7F2A51B894CC4F348DC5C2B840772F36RHM3',
  log: 'FORM-ACD427B1D88545F5BCC4B20DB6B4BD9FJVAX',
  safeLog: 'FORM-7FC88BAA352945F29E30B337BCFD49C97TBT',
  station: 'FORM-AF24B9440B5B41F1AC830065140D73FB1XBK',
  hazard: 'FORM-04BDB63138D34DDE9A1330EEBD550473E4HJ'
};
var F = {
  pName: 'textField_mj7z6v5p',
  pAddr: 'textField_mkktvlmv',
  pDirector: 'employeeField_mj803km2',
  pEngineer: 'employeeField_mj803km0',
  dPN: 'textField_ml6no8vf',
  dCat: 'cascadeSelectField_ml6no8w5',
  dDate: 'dateField_ml6no8wb',
  dSub: 'employeeField_ml6no8vd',
  dtPN: 'textField_mlg9av31',
  dtTitle: 'textField_mlgckllq',
  dtDate: 'dateField_mlelkrk3',
  dtPub: 'employeeField_mlenlbgs',
  lPN: 'textField_mjjngk77',
  lDate: 'dateField_mjjngk7x',
  lSub: 'employeeField_mjmamg0o',
  sfPN: 'textField_mjjngk77',
  sfDate: 'dateField_mjjngk7x',
  sfSub: 'employeeField_mjmamg0o',
  stPN: 'textField_mj89asvv',
  stDate: 'dateField_mjl61srf',
  stKey: 'textField_mjkqt2ls',
  stSub: 'employeeField_mj89asvr',
  hzPN: 'textField_mj89asvv',
  hzDate: 'dateField_mpuvsdbz',
  hzSub: 'employeeField_mpuvsdc4',
  hzLevel: 'radioField_mpuvsdbx',
  hzStatus: 'radioField_mpumsa4p'
};
var _state = {
  projects: [],
  selProj: '',
  selProjInfo: null,
  dateStart: '',
  dateEnd: '',
  rangeType: 'thisWeek',
  loading: false,
  data: {},
  stats: {},
  selYear: 0,
  selMonth: -1,
  selWeekIdx: 0,
  weeksInMonth: [],
  isMonthMode: false
};
var BASE_URL = 'https://h7wohg.aliwork.com',
  APP_TYPE = 'APP_ULZ41JLFCK1R3DPUXEDS';
// 折叠状态
var _collapsed = {};
// debug
var _log = '';
function pad(n) {
  return ('0' + n).slice(-2);
}
function fmtDate(d) {
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}
function parseTs(s) {
  if (!s) return 0;
  var p = s.split('-');
  if (p.length < 3) return 0;
  return new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10)).getTime();
}
function tsToDate(ts) {
  if (!ts) return '-';
  return fmtDate(new Date(ts));
}
function ev(fd, fid) {
  if (!fd || !fid) return null;
  var f = fd[fid];
  if (!f) return null;
  if (typeof f === 'string' || typeof f === 'number') return String(f);
  if (Array.isArray(f.value)) {
    var a = [];
    for (var i = 0; i < f.value.length; i++) {
      var v = f.value[i];
      a.push(typeof v === 'object' && v !== null ? v.name || String(v) : String(v));
    }
    return a.join('、');
  }
  if (f.value !== null && f.value !== undefined) {
    var v = f.value;
    if (typeof v === 'object' && v !== null && v.label) return v.label;
    return String(v);
  }
  if (Array.isArray(f)) {
    var a = [];
    for (var i = 0; i < f.length; i++) {
      var v = f[i];
      a.push(typeof v === 'object' && v !== null ? v.name || String(v) : String(v));
    }
    return a.join('、');
  }
  return null;
}
function getCascadeLabel(fd, fid) {
  if (!fd || !fid) return '未分类';
  var f = fd[fid];
  if (!f) return '未分类';
  // 兼容两种格式：{value:["设计图纸"]} 或 直接 ["设计图纸"]
  var v = f.value !== undefined ? f.value : f;
  if (!Array.isArray(v)) v = [v];
  var items = v.filter(x => {
    return x !== null && x !== undefined && x !== '';
  }).map(x => {
    return typeof x === 'object' ? x.key || x.label || String(x) : String(x);
  });
  return items.length > 0 ? items.join(' > ') : '未分类';
}
function getEmpName(fd, fid) {
  if (!fd || !fid) return '';
  var f = fd[fid];
  if (!f) return '';
  // EmployeeField 可能直接在字段上（没有 .value 包装）
  if (Array.isArray(f)) {
    if (!f.length) return '';
    var first = f[0];
    if (typeof first === 'object' && first !== null) return first.label || first.name || String(first);
    return String(first);
  }
  if (typeof f === 'object' && f !== null && f.label) return f.label;
  if (typeof f === 'object' && f !== null && f.name) return f.name;
  var v = f.value;
  if (Array.isArray(v)) {
    if (!v.length) return '';
    var first = v[0];
    if (typeof first === 'object' && first !== null) return first.label || first.name || String(first);
    return String(first);
  }
  if (typeof v === 'object' && v !== null) return v.label || v.name || '';
  return String(v || '');
}
// 从 formData 中获取并格式化日期字段值
function getDateStr(fd, fid) {
  if (!fd || !fid) return '-';
  var f = fd[fid];
  if (!f) return '-';
  // 兼容 f 直接是值（没有 .value 包装）
  if (typeof f === 'number') return tsToDate(f);
  if (typeof f === 'string') {
    if (/^\d+$/.test(f)) return tsToDate(parseInt(f, 10));
    return f;
  }
  var v = f.value;
  if (v === null || v === undefined) return '-';
  if (typeof v === 'number') return tsToDate(v);
  if (typeof v === 'string') {
    if (/^\d+$/.test(v)) return tsToDate(parseInt(v, 10));
    return v;
  }
  return String(v);
}
function extractData(res) {
  return res && res.data || res && res.content && res.content.data || [];
}
// 计算指定年月包含的所有周（周一~周日），返回 {label,start,end,idx}[]
function calcWeeksInMonth(year, month) {
  // month: 0-based (0=Jan)
  var firstDay = new Date(year, month, 1);
  var lastDay = new Date(year, month + 1, 0);
  // 当月1号所在周的周一
  var dow = firstDay.getDay(); // 0=Sun
  var monOff = dow === 0 ? -6 : 1 - dow;
  var monday = new Date(firstDay);
  monday.setDate(firstDay.getDate() + monOff);
  var weeks = [],
    wn = 1,
    cur = new Date(monday);
  while (true) {
    var ws = new Date(cur),
      we = new Date(cur);
    we.setDate(cur.getDate() + 6);
    if (we < firstDay) {
      cur.setDate(cur.getDate() + 7);
      continue;
    }
    if (ws > lastDay) break;
    weeks.push({
      label: '第' + wn + '周（' + fmtDate(ws) + '~' + fmtDate(we) + '）',
      start: fmtDate(ws),
      end: fmtDate(we),
      idx: wn
    });
    wn++;
    cur.setDate(cur.getDate() + 7);
  }
  return weeks;
}
// 找到包含给定日期的周在 weeks 数组中的索引
function findWeekIndex(weeks, dateStr) {
  for (var i = 0; i < weeks.length; i++) {
    if (dateStr >= weeks[i].start && dateStr <= weeks[i].end) return i;
  }
  return 0;
}
function getUrlParams() {
  var p = {};
  var s = window.location.search || '';
  if (s.charAt(0) === '?') s = s.substring(1);
  s.split('&').forEach(kv => {
    var parts = kv.split('=');
    if (parts.length >= 2) p[decodeURIComponent(parts[0])] = decodeURIComponent(parts.slice(1).join('='));
  });
  return p;
}
export function getCustomState(k) {
  return k ? _state[k] : Object.assign({}, _state);
}
export function setCustomState(ns) {
  Object.keys(ns).forEach(k => {
    _state[k] = ns[k];
  });
  this.setState({
    t: Date.now()
  });
}
export function forceUpdate() {
  this.setState({
    t: Date.now()
  });
}

// ===== 生命周期 =====
export function didMount() {
  var S = this;
  var urlP = getUrlParams();
  var targetId = urlP.projId || null;
  // 加载项目列表
  S.utils.yida.searchFormDatas({
    formUuid: FU.project,
    pageSize: 100,
    currentPage: 1
  }).then(function (r) {
    var raw = extractData(r);
    var ps = [];
    for (var i = 0; i < raw.length; i++) {
      var fd = raw[i].formData || {};
      var nm = ev(fd, F.pName) || '';
      if (nm.indexOf('测试') > -1 || nm.toLowerCase().indexOf('test') > -1) continue;
      ps.push({
        instId: raw[i].formInstId || '',
        name: nm,
        director: ev(fd, F.pDirector) || '-',
        engineer: ev(fd, F.pEngineer) || '-',
        addr: ev(fd, F.pAddr) || '-'
      });
    }
    // 如果 URL 中有 projId，定位到对应项目
    if (targetId) {
      for (var j = 0; j < ps.length; j++) {
        if (ps[j].instId === targetId) {
          _state.selProj = ps[j].name;
          _state.selProjInfo = ps[j];
          break;
        }
      }
    }
    _state.projects = ps;
    // 初始化级联时间：当前年月
    var now = new Date();
    _state.selYear = now.getFullYear();
    _state.selMonth = now.getMonth();
    S.onMonthChanged();
    S.setState({
      t: Date.now()
    });
    if (_state.selProj) S.loadStats();
  }).catch(function () {
    S.setState({
      t: Date.now()
    });
  });
}

// ===== 级联时间选择 =====
// 年变化
export function onYearChange(e) {
  _state.selYear = parseInt(e.target.value, 10);
  this.onMonthChanged();
  var S = this;
  S.setState({
    t: Date.now()
  });
  if (_state.selProj) S.loadStats();
}
// 月变化：重新计算该月的周列表，最前面加上"月报"选项
export function onMonthChanged() {
  var y = _state.selYear,
    m = _state.selMonth;
  if (m < 0) {
    _state.weeksInMonth = [];
    return;
  }
  var weeks = calcWeeksInMonth(y, m);
  // 在最前面插入"月报（整月）"条目
  var monthStart = fmtDate(new Date(y, m, 1));
  var monthEnd = fmtDate(new Date(y, m + 1, 0));
  weeks.unshift({
    label: '月报（' + monthStart + '~' + monthEnd + '）',
    start: monthStart,
    end: monthEnd,
    idx: 0,
    isMonth: true
  });
  _state.weeksInMonth = weeks;
  // 默认选中月报
  _state.selWeekIdx = 0;
  _state.isMonthMode = true;
  _state.dateStart = weeks[0].start;
  _state.dateEnd = weeks[0].end;
}
export function onMonthChange(e) {
  _state.selMonth = parseInt(e.target.value, 10);
  this.onMonthChanged();
  var S = this;
  S.setState({
    t: Date.now()
  });
  if (_state.selProj) S.loadStats();
}
export function onWeekChange(e) {
  var idx = parseInt(e.target.value, 10);
  _state.selWeekIdx = idx;
  _state.isMonthMode = idx === 0;
  _state.dateStart = _state.weeksInMonth[idx].start;
  _state.dateEnd = _state.weeksInMonth[idx].end;
  var S = this;
  S.setState({
    t: Date.now()
  });
  if (_state.selProj) S.loadStats();
}
export function onProjChange(e) {
  _state.selProj = e.target.value;
  // 找到项目信息
  for (var i = 0; i < _state.projects.length; i++) {
    if (_state.projects[i].name === e.target.value) {
      _state.selProjInfo = _state.projects[i];
      break;
    }
  }
  this.setState({
    t: Date.now()
  });
}
export function toggleCollapse(k) {
  _collapsed[k] = !_collapsed[k];
  this.setState({
    t: Date.now()
  });
}

// ===== 全量分页拉取 =====
export function fetchAllPages(formUuid, sfj) {
  var S = this;
  return new Promise(function (resolve) {
    var all = [];
    S.utils.yida.searchFormDatas({
      formUuid: formUuid,
      searchFieldJson: sfj,
      pageSize: 100,
      currentPage: 1
    }).then(function (r) {
      var d = extractData(r);
      all = all.concat(d);
      var total = 0;
      if (r && typeof r.totalCount === 'number') total = r.totalCount;else if (r && r.content && typeof r.content.totalCount === 'number') total = r.content.totalCount;else total = d.length;
      var pages = Math.ceil(total / 100);
      if (pages <= 1) {
        resolve(all);
        return;
      }
      var tasks = [];
      for (var i = 2; i <= pages; i++) {
        tasks.push(S.utils.yida.searchFormDatas({
          formUuid: formUuid,
          searchFieldJson: sfj,
          pageSize: 100,
          currentPage: i
        }));
      }
      return Promise.all(tasks);
    }).then(function (rs) {
      if (!rs) return;
      for (var i = 0; i < rs.length; i++) {
        all = all.concat(extractData(rs[i]));
      }
      resolve(all);
    }).catch(function () {
      resolve(all);
    });
  });
}

// ===== 加载数据 =====
export function loadStats() {
  var S = this;
  var pn = _state.selProj;
  if (!pn) {
    return;
  }
  _state.loading = true;
  _state.data = {};
  _state.stats = {};
  S.setState({
    t: Date.now()
  });
  var ds = _state.dateStart,
    de = _state.dateEnd;
  function makeSFJ(pf, df) {
    var o = {};
    o[pf] = _state.selProj;
    if (_state.dateStart && _state.dateEnd && df) o[df] = [parseTs(_state.dateStart), parseTs(_state.dateEnd) + 86399999];
    return JSON.stringify(o);
  }
  var p1 = S.fetchAllPages(FU.docLib, makeSFJ(F.dPN, F.dDate));
  var p2 = S.fetchAllPages(FU.dynamic, makeSFJ(F.dtPN, F.dtDate));
  var p3 = S.fetchAllPages(FU.log, makeSFJ(F.lPN, F.lDate));
  var p4 = S.fetchAllPages(FU.safeLog, makeSFJ(F.sfPN, F.sfDate));
  var p5 = S.fetchAllPages(FU.station, makeSFJ(F.stPN, F.stDate));
  var p6 = S.fetchAllPages(FU.hazard, makeSFJ(F.hzPN, F.hzDate));
  Promise.all([p1, p2, p3, p4, p5, p6]).then(function (rs) {
    _state.data = {
      docLib: rs[0],
      dynamic: rs[1],
      log: rs[2],
      safeLog: rs[3],
      station: rs[4],
      hazard: rs[5]
    };
    _state.loading = false;
    S.setState({
      t: Date.now()
    });
  }).catch(function () {
    _state.loading = false;
    S.setState({
      t: Date.now()
    });
  });
}

// ===== 跳转详情 =====
export function goDetail(formUuid, item) {
  var instId = item.formInstId || item.instanceId || '';
  if (instId) {
    window.open(BASE_URL + '/' + APP_TYPE + '/formDetail/' + formUuid + '?formInstId=' + instId, '_blank');
  }
}

// ===== 渲染 =====
export function renderJsx() {
  var S = this;
  var t = this.state && this.state.t;
  var isM = this.utils.isMobile();
  var s = _state;
  var ds = s.dateStart,
    de = s.dateEnd;
  var Sty = {
    ct: {
      padding: isM ? '10px 10px' : '16px 20px',
      maxWidth: '1200px',
      margin: '0 auto',
      fontFamily: '-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif',
      borderRadius: '0 !important',
      background: '#f0f2f5',
      minHeight: '100vh'
    },
    cd: {
      background: '#fff',
      borderRadius: '10px',
      padding: isM ? '12px 12px' : '16px 20px',
      marginBottom: '12px',
      boxShadow: '0 1px 3px rgba(0,0,0,0.06)'
    },
    sel: {
      padding: '6px 10px',
      border: '1px solid #e0e0e0',
      borderRadius: '6px',
      fontSize: '14px',
      background: '#fff',
      width: isM ? '100%' : '600px'
    },
    btn: {
      padding: '6px 14px',
      border: '1px solid #d9d9d9',
      borderRadius: '6px',
      fontSize: '14px',
      cursor: 'pointer',
      background: '#fff',
      color: '#555'
    },
    btnA: {
      padding: '6px 14px',
      border: '1px solid #4f46e5',
      borderRadius: '6px',
      fontSize: '14px',
      cursor: 'pointer',
      background: '#4f46e5',
      color: '#fff'
    },
    dateIn: {
      padding: '6px 10px',
      border: '1px solid #d9d9d9',
      borderRadius: '6px',
      fontSize: '14px',
      width: '130px'
    },
    sBtn: {
      padding: '6px 24px',
      border: 'none',
      borderRadius: '6px',
      fontSize: '14px',
      fontWeight: 600,
      cursor: 'pointer',
      background: 'linear-gradient(135deg,#3b82f6,#2563eb)',
      color: '#fff'
    },
    sc: {
      background: '#fff',
      borderRadius: '10px',
      padding: '14px 16px',
      marginBottom: '10px',
      boxShadow: '0 1px 3px rgba(0,0,0,0.06)'
    },
    sh: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '12px 16px',
      cursor: 'pointer',
      userSelect: 'none',
      borderBottom: '1px solid #f0f0f0'
    },
    sb: {
      padding: '12px 16px'
    },
    tb: {
      width: '100%',
      borderCollapse: 'collapse',
      fontSize: '14px',
      border: '1px solid #f0f0f0',
      borderRadius: '6px',
      overflow: 'hidden'
    },
    th: {
      padding: '6px 10px',
      textAlign: 'left',
      fontWeight: 600,
      color: '#595959',
      fontSize: '14px',
      background: '#f8f9fb',
      borderBottom: '1px solid #f0f0f0'
    },
    td: {
      padding: '6px 10px',
      borderBottom: '1px solid #f5f5f5',
      color: '#262626',
      fontSize: '14px'
    },
    lnk: {
      color: '#4f46e5',
      textDecoration: 'none',
      cursor: 'pointer',
      fontSize: '14px'
    },
    tag: {
      display: 'inline-block',
      fontSize: '12px',
      padding: '2px 8px',
      borderRadius: '4px',
      fontWeight: 500,
      margin: '1px'
    },
    bg: {
      display: 'inline-block',
      fontSize: '12px',
      background: '#eef2ff',
      color: '#4f46e5',
      padding: '2px 8px',
      borderRadius: '8px',
      fontWeight: 500
    }
  };
  var rangeLabels = {
    today: '今日',
    yesterday: '昨日',
    thisWeek: '本周',
    lastWeek: '上周',
    thisMonth: '本月',
    lastMonth: '上月'
  };

  // 统计各模块数据量
  var srcKeys = ['docLib', 'dynamic', 'log', 'safeLog', 'station', 'hazard'];
  var srcNames = {
    docLib: '资料库',
    dynamic: '项目动态',
    log: '监理日志',
    safeLog: '日志(安全)',
    station: '旁站记录',
    hazard: '安全隐患台账'
  };
  function countRecords(key) {
    return (s.data[key] || []).length;
  }
  return <div style={Sty.ct}><div style={{
      display: "none"
    }}>{this.state && this.state.timestamp}</div>
      <div style={{
      display: 'none'
    }}>{t}</div>

      {/* 筛选区 */}
      <div style={Sty.cd}>
        <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        flexWrap: 'wrap',
        marginBottom: '10px'
      }}>
          <span style={{
          fontWeight: 600,
          fontSize: '16px'
        }}>📋 项目：</span>
          <select style={Sty.sel} value={s.selProj} onChange={function (e) {
          S.onProjChange(e);
        }}>
            <option value="">-- 请选择项目 --</option>
            {s.projects.map((p, idx) => {
            return <option key={idx} value={p.name}>{p.name}</option>;
          })}
          </select>
        </div>
        <div style={{
        display: 'flex',
        gap: '6px',
        flexWrap: 'wrap',
        alignItems: 'center',
        marginBottom: '8px'
      }}>
          <span style={{
          fontWeight: 500,
          fontSize: '14px',
          color: '#595959'
        }}>时间：</span>
          <select style={{
          padding: '6px 10px',
          border: '1px solid #e0e0e0',
          borderRadius: '6px',
          fontSize: '14px',
          background: '#fff'
        }} value={s.selYear} onChange={function (e) {
          S.onYearChange(e);
        }}>
            {function () {
            var opts = [];
            for (var y = 2024; y <= 2030; y++) {
              opts.push(<option key={y} value={y}>{y}年</option>);
            }
            return opts;
          }()}
          </select>
          <select style={{
          padding: '6px 10px',
          border: '1px solid #e0e0e0',
          borderRadius: '6px',
          fontSize: '14px',
          background: '#fff'
        }} value={s.selMonth} onChange={function (e) {
          S.onMonthChange(e);
        }}>
            {function () {
            var months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
            return months.map((m, i) => {
              return <option key={i} value={i}>{m}</option>;
            });
          }()}
          </select>
          {s.weeksInMonth.length > 0 ? <select style={{
          padding: '6px 10px',
          border: '1px solid #e0e0e0',
          borderRadius: '6px',
          fontSize: '14px',
          background: '#fff',
          minWidth: '240px'
        }} value={s.selWeekIdx} onChange={function (e) {
          S.onWeekChange(e);
        }}>
            {s.weeksInMonth.map((w, i) => {
            return <option key={i} value={i}>{w.label}</option>;
          })}
          </select> : null}
          {s.projects.length > 0 ? <button style={Sty.sBtn} onClick={function () {
          S.loadStats();
        }}>查询</button> : null}
        </div>
        {s.dateStart ? <div style={{
        fontSize: '11px',
        color: '#8c8c8c'
      }}>统计范围：{s.dateStart} ~ {s.dateEnd}</div> : null}
      </div>

      {/* 加载中 */}
      {s.loading ? <div style={{
      textAlign: 'center',
      padding: '40px',
      color: '#8c8c8c'
    }}>数据加载中...</div> : !s.selProj ? <div style={{
      textAlign: 'center',
      padding: '40px',
      color: '#8c8c8c'
    }}>请选择项目并点击查询</div> : <div>
        {/* 项目基本信息 */}
        {function () {
        var info = s.selProjInfo;
        var isM2 = this.utils && this.utils.isMobile();
        return info ? <div style={Sty.cd}>
            <div style={{
            fontSize: '16px',
            fontWeight: 700,
            marginBottom: '10px',
            borderBottom: '2px solid #eef2ff',
            paddingBottom: '8px'
          }}>项目信息</div>
            <div style={{
            display: 'grid',
            gridTemplateColumns: isM2 ? '1fr' : '1fr 1fr',
            gap: '6px 20px'
          }}>
              <div style={{
              display: 'flex',
              alignItems: 'baseline',
              gap: '4px',
              padding: '4px 0',
              borderBottom: '1px dashed #f0f0f0'
            }}><span style={{
                fontSize: '14px',
                color: '#8c8c8c',
                minWidth: '70px',
                flexShrink: 0
              }}>报告时间</span><span style={{
                fontSize: '14px',
                color: '#1a1a2e',
                fontWeight: 500
              }}>{s.dateStart} ~ {s.dateEnd}</span></div>
              <div style={{
              display: 'flex',
              alignItems: 'baseline',
              gap: '4px',
              padding: '4px 0',
              borderBottom: '1px dashed #f0f0f0'
            }}><span style={{
                fontSize: '14px',
                color: '#8c8c8c',
                minWidth: '70px',
                flexShrink: 0
              }}>负责人</span><span style={{
                fontSize: '14px',
                color: '#1a1a2e',
                fontWeight: 500
              }}>-</span></div>
              <div style={{
              display: 'flex',
              alignItems: 'baseline',
              gap: '4px',
              padding: '4px 0',
              borderBottom: '1px dashed #f0f0f0'
            }}><span style={{
                fontSize: '14px',
                color: '#8c8c8c',
                minWidth: '70px',
                flexShrink: 0
              }}>项目地址</span><span style={{
                fontSize: '14px',
                color: '#1a1a2e',
                fontWeight: 500
              }}>{info.addr}</span></div>
              <div style={{
              display: 'flex',
              alignItems: 'baseline',
              gap: '4px',
              padding: '4px 0',
              borderBottom: '1px dashed #f0f0f0'
            }}><span style={{
                fontSize: '14px',
                color: '#8c8c8c',
                minWidth: '70px',
                flexShrink: 0
              }}>人员</span><span style={{
                fontSize: '14px',
                color: '#1a1a2e',
                fontWeight: 500,
                display: 'flex',
                flexWrap: 'wrap',
                gap: '3px'
              }}>
                {info.director !== '-' ? <span style={{
                  display: 'inline-block',
                  fontSize: '12px',
                  padding: '2px 8px',
                  borderRadius: '4px',
                  background: '#f5f3ff',
                  color: '#7c3aed'
                }}>{info.director}（总监）</span> : null}
                {info.engineer !== '-' ? <span style={{
                  display: 'inline-block',
                  fontSize: '12px',
                  padding: '2px 8px',
                  borderRadius: '4px',
                  background: '#f5f3ff',
                  color: '#7c3aed'
                }}>{info.engineer}（专监）</span> : null}
              </span></div>
            </div>
          </div> : null;
      }.bind(this)()}

        {/* 各数据源统计 */}
        <div style={Sty.cd}>
          <div style={{
          fontSize: '16px',
          fontWeight: 700,
          marginBottom: '10px',
          borderBottom: '2px solid #eef2ff',
          paddingBottom: '8px'
        }}>📊 各数据源统计</div>
          <div style={{
          display: 'flex',
          gap: '8px',
          flexWrap: 'wrap',
          marginBottom: '12px'
        }}>
          {srcKeys.map((k, idx) => {
            var cnt = countRecords(k);
            var colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#14b8a6'];
            return <div key={k} style={{
              flex: '1 1 80px',
              textAlign: 'center',
              padding: '12px 8px',
              background: '#fff',
              borderRadius: '8px',
              border: '1px solid #f0f0f0',
              boxShadow: '0 1px 2px rgba(0,0,0,0.04)'
            }}>
              <div style={{
                fontSize: '14px',
                color: '#8c8c8c',
                marginBottom: '4px'
              }}>{srcNames[k]}</div>
              <div style={{
                fontSize: '18px',
                fontWeight: 700,
                color: colors[idx]
              }}>{cnt}</div>
            </div>;
          })}
        </div>

        {/* ===== 资料库 ===== */}
        {s.data.docLib ? <div style={Sty.sc}>
          <div style={Sty.sh} onClick={function (e) {
            S.toggleCollapse('doc');
          }}>
            <span style={{
              fontWeight: 700,
              fontSize: '16px'
            }}>📚 资料库 <span style={Sty.bg}>{s.data.docLib.length} 条</span></span>
            <span style={{
              transform: _collapsed.doc ? 'rotate(-90deg)' : '',
              fontSize: '11px',
              color: '#999'
            }}>▼</span>
          </div>
          {!_collapsed.doc ? <div style={Sty.sb}>
            {/* 按分类分组 */}
            {function () {
              var groups = {};
              s.data.docLib.forEach(item => {
                var fd = item.formData || {};
                var cat = getCascadeLabel(fd, F.dCat);
                if (!groups[cat]) groups[cat] = [];
                groups[cat].push(item);
              });
              var cats = Object.keys(groups).sort();
              return cats.map(cat => {
                var items = groups[cat];
                return <div key={cat} style={{
                  marginBottom: '10px'
                }}>
                  <div style={{
                    fontSize: '14px',
                    fontWeight: 600,
                    color: '#4f46e5',
                    marginBottom: '6px',
                    borderBottom: '1px dashed #e8e8e8',
                    paddingBottom: '4px'
                  }}>{cat} <span style={Sty.bg}>{items.length} 条</span></div>
                  <table style={Sty.tb}>
                    <thead><tr><th style={Sty.th}>提交人</th><th style={Sty.th}>日期</th></tr></thead>
                    <tbody>{items.map((item, ri) => {
                        var fd = item.formData || {};
                        return <tr key={ri}>
                        <td style={Sty.td}>{getEmpName(fd, F.dSub) || '-'}</td>
                        <td style={Sty.td}><a style={Sty.lnk} onClick={function () {
                              S.goDetail(FU.docLib, item);
                            }}>{getDateStr(fd, F.dDate)}</a></td>
                      </tr>;
                      })}</tbody>
                  </table>
                </div>;
              });
            }()}
          </div> : null}
        </div> : null}

        {/* ===== 项目动态 ===== */}
        {s.data.dynamic ? <div style={Sty.sc}>
          <div style={Sty.sh} onClick={function (e) {
            S.toggleCollapse('dynamic');
          }}>
            <span style={{
              fontWeight: 700,
              fontSize: '16px'
            }}>📊 项目动态 <span style={Sty.bg}>{s.data.dynamic.length} 条</span></span>
            <span style={{
              transform: _collapsed.dynamic ? 'rotate(-90deg)' : '',
              fontSize: '11px',
              color: '#999'
            }}>▼</span>
          </div>
          {!_collapsed.dynamic ? <div style={Sty.sb}>
            <table style={Sty.tb}>
              <thead><tr><th style={Sty.th}>提交人</th><th style={Sty.th}>标题/内容</th><th style={Sty.th}>日期</th></tr></thead>
              <tbody>{s.data.dynamic.map((item, ri) => {
                  var fd = item.formData || {};
                  return <tr key={ri}><td style={Sty.td}>{getEmpName(fd, F.dtPub) || '-'}</td><td style={Sty.td}>{ev(fd, F.dtTitle) || '-'}</td><td style={Sty.td}><a style={Sty.lnk} onClick={function () {
                        S.goDetail(FU.dynamic, item);
                      }}>{getDateStr(fd, F.dtDate)}</a></td></tr>;
                })}</tbody>
            </table>
          </div> : null}
        </div> : null}

        {/* ===== 监理日志 ===== */}
        {s.data.log ? <div style={Sty.sc}>
          <div style={Sty.sh} onClick={function (e) {
            S.toggleCollapse('log');
          }}>
            <span style={{
              fontWeight: 700,
              fontSize: '16px'
            }}>📋 监理日志 <span style={Sty.bg}>{s.data.log.length} 条</span></span>
            <span style={{
              transform: _collapsed.log ? 'rotate(-90deg)' : '',
              fontSize: '11px',
              color: '#999'
            }}>▼</span>
          </div>
          {!_collapsed.log ? <div style={Sty.sb}>
            <table style={Sty.tb}>
              <thead><tr><th style={Sty.th}>提交人</th><th style={Sty.th}>日期</th></tr></thead>
              <tbody>{s.data.log.map((item, ri) => {
                  var fd = item.formData || {};
                  return <tr key={ri}><td style={Sty.td}>{getEmpName(fd, F.lSub) || '-'}</td><td style={Sty.td}><a style={Sty.lnk} onClick={function () {
                        S.goDetail(FU.log, item);
                      }}>{getDateStr(fd, F.lDate)}</a></td></tr>;
                })}</tbody>
            </table>
          </div> : null}
        </div> : null}

        {/* ===== 监理日志(安全) ===== */}
        {s.data.safeLog ? <div style={Sty.sc}>
          <div style={Sty.sh} onClick={function (e) {
            S.toggleCollapse('safeLog');
          }}>
            <span style={{
              fontWeight: 700,
              fontSize: '16px'
            }}>🛡️ 监理日志（安全） <span style={Sty.bg}>{s.data.safeLog.length} 条</span></span>
            <span style={{
              transform: _collapsed.safeLog ? 'rotate(-90deg)' : '',
              fontSize: '11px',
              color: '#999'
            }}>▼</span>
          </div>
          {!_collapsed.safeLog ? <div style={Sty.sb}>
            <table style={Sty.tb}>
              <thead><tr><th style={Sty.th}>提交人</th><th style={Sty.th}>日期</th></tr></thead>
              <tbody>{s.data.safeLog.map((item, ri) => {
                  var fd = item.formData || {};
                  return <tr key={ri}><td style={Sty.td}>{getEmpName(fd, F.sfSub) || '-'}</td><td style={Sty.td}><a style={Sty.lnk} onClick={function () {
                        S.goDetail(FU.safeLog, item);
                      }}>{getDateStr(fd, F.sfDate)}</a></td></tr>;
                })}</tbody>
            </table>
          </div> : null}
        </div> : null}

        {/* ===== 旁站记录 ===== */}
        {s.data.station ? <div style={Sty.sc}>
          <div style={Sty.sh} onClick={function (e) {
            S.toggleCollapse('station');
          }}>
            <span style={{
              fontWeight: 700,
              fontSize: '16px'
            }}>🔍 旁站记录 <span style={Sty.bg}>{s.data.station.length} 条</span></span>
            <span style={{
              transform: _collapsed.station ? 'rotate(-90deg)' : '',
              fontSize: '11px',
              color: '#999'
            }}>▼</span>
          </div>
          {!_collapsed.station ? <div style={Sty.sb}>
            <table style={Sty.tb}>
              <thead><tr><th style={Sty.th}>提交人</th><th style={Sty.th}>日期</th></tr></thead>
              <tbody>{s.data.station.map((item, ri) => {
                  var fd = item.formData || {};
                  return <tr key={ri}><td style={Sty.td}>{getEmpName(fd, F.stSub) || '-'}</td><td style={Sty.td}><a style={Sty.lnk} onClick={function () {
                        S.goDetail(FU.station, item);
                      }}>{getDateStr(fd, F.stDate)}</a></td></tr>;
                })}</tbody>
            </table>
          </div> : null}
        </div> : null}

        {/* ===== 安全隐患台账 ===== */}
        {s.data.hazard ? <div style={Sty.sc}>
          <div style={Sty.sh} onClick={function (e) {
            S.toggleCollapse('hazard');
          }}>
            <span style={{
              fontWeight: 700,
              fontSize: '16px'
            }}>⚠️ 安全隐患台账 <span style={Sty.bg}>{s.data.hazard.length} 条</span></span>
            <span style={{
              transform: _collapsed.hazard ? 'rotate(-90deg)' : '',
              fontSize: '11px',
              color: '#999'
            }}>▼</span>
          </div>
          {!_collapsed.hazard ? <div style={Sty.sb}>
            <table style={Sty.tb}>
              <thead><tr><th style={Sty.th}>提交人</th><th style={Sty.th}>隐患等级</th><th style={Sty.th}>安全隐患状态</th><th style={Sty.th}>日期</th></tr></thead>
              <tbody>{s.data.hazard.map((item, ri) => {
                  var fd = item.formData || {};
                  var lv = ev(fd, F.hzLevel) || '-';
                  var st = ev(fd, F.hzStatus) || '-';
                  return <tr key={ri}>
                  <td style={Sty.td}>{getEmpName(fd, F.hzSub) || '-'}</td>
                  <td style={Sty.td}><span style={Object.assign({}, Sty.tag, {
                        background: lv === '重大' ? '#fef2f2' : lv === '较大' ? '#fff7ed' : '#eef2ff',
                        color: lv === '重大' ? '#dc2626' : lv === '较大' ? '#d97706' : '#4f46e5'
                      })}>{lv}</span></td>
                  <td style={Sty.td}><span style={Object.assign({}, Sty.tag, {
                        background: st === '已整改' ? '#ecfdf5' : st === '整改中' ? '#eef2ff' : '#fef3c7',
                        color: st === '已整改' ? '#059669' : st === '整改中' ? '#4f46e5' : '#b45309'
                      })}>{st}</span></td>
                  <td style={Sty.td}><a style={Sty.lnk} onClick={function () {
                        S.goDetail(FU.hazard, item);
                      }}>{getDateStr(fd, F.hzDate)}</a></td>
                </tr>;
                })}</tbody>
            </table>
          </div> : null}
        </div> : null}

        {/* 无数据提示 */}
        {srcKeys.every(k => {
          return (s.data[k] || []).length === 0;
        }) ? <div style={{
          textAlign: 'center',
          padding: '40px',
          color: '#8c8c8c'
        }}>该时间范围内暂无数据</div> : null}
      </div></div>}
    </div>;
}