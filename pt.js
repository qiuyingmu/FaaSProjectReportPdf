// ============================================================
// 平台运营报告 - v16
// - 4个时间范围：本周/上周/本月/上月
// - 月度显示格式：2026年7月
// - 项目清单简洁化（去icon）
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
  dPN: 'textField_ml6no8vf',
  dDate: 'dateField_ml6no8wb',
  dtPN: 'textField_mlg9av31',
  dtDate: 'dateField_mlelkrk3',
  lPN: 'textField_mjjngk77',
  lDate: 'dateField_mjjngk7x',
  sfPN: 'textField_mjjngk77',
  sfDate: 'dateField_mjjngk7x',
  stPN: 'textField_mj89asvv',
  stDate: 'dateField_mjl61srf',
  hzPN: 'textField_mj89asvv',
  hzDate: 'dateField_mpuvsdbz'
};
var SRCS = [{
  k: 'docLib',
  l: '资料库',
  c: '#3b82f6',
  u: FU.docLib,
  f: F.dPN,
  d: F.dDate
}, {
  k: 'dynamic',
  l: '项目动态',
  c: '#10b981',
  u: FU.dynamic,
  f: F.dtPN,
  d: F.dtDate
}, {
  k: 'log',
  l: '监理日志',
  c: '#f59e0b',
  u: FU.log,
  f: F.lPN,
  d: F.lDate
}, {
  k: 'safeLog',
  l: '日志(安全)',
  c: '#8b5cf6',
  u: FU.safeLog,
  f: F.sfPN,
  d: F.sfDate
}, {
  k: 'station',
  l: '旁站记录',
  c: '#ef4444',
  u: FU.station,
  f: F.stPN,
  d: F.stDate
}, {
  k: 'hazard',
  l: '安全隐患',
  c: '#14b8a6',
  u: FU.hazard,
  f: F.hzPN,
  d: F.hzDate
}];
var _P = [],
  _R = 'week',
  _L = true,
  _stats = {};
function pad(n) {
  return ('0' + n).slice(-2);
}
function fd(d) {
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

// 本周
function ws(d) {
  var x = d.getDay() || 7;
  var m = new Date(d);
  m.setDate(m.getDate() - x + 1);
  m.setHours(0, 0, 0, 0);
  return m;
}
function we(d) {
  var s = ws(d);
  var e = new Date(s);
  e.setDate(s.getDate() + 6);
  return e;
}
// 上周
function lws(d) {
  var x = d.getDay() || 7;
  var m = new Date(d);
  m.setDate(m.getDate() - x - 6);
  m.setHours(0, 0, 0, 0);
  return m;
}
function lwe(d) {
  var s = lws(d);
  var e = new Date(s);
  e.setDate(s.getDate() + 6);
  return e;
}
function iw(d) {
  var t = new Date(d);
  var dn = (d.getDay() + 6) % 7;
  t.setDate(t.getDate() - dn + 3);
  var ft = t.valueOf();
  t.setMonth(0, 1);
  if (t.getDay() !== 4) t.setMonth(0, 1 + (4 - t.getDay() + 7) % 7);
  return 1 + Math.ceil((ft - t) / 604800000);
}
function getRange(range) {
  var n = new Date();
  if (range === 'week') return {
    s: ws(n),
    e: n,
    lb: ws(n),
    le: we(n)
  };
  if (range === 'lastWeek') return {
    s: lws(n),
    e: lwe(n),
    lb: lws(n),
    le: lwe(n)
  };
  if (range === 'month') return {
    s: new Date(n.getFullYear(), n.getMonth(), 1),
    e: n
  };
  // lastMonth
  var pm = new Date(n.getFullYear(), n.getMonth() - 1, 1);
  var pe = new Date(n.getFullYear(), n.getMonth(), 0, 23, 59, 59, 999);
  return {
    s: pm,
    e: pe
  };
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
function extractDistrict(addr) {
  if (!addr) return '-';
  var m = addr.match(/([^\s,，、]+?区)/);
  if (m) return m[1];
  var m2 = addr.match(/([^\s,，、]+?县)(?![区市])/);
  if (m2) return m2[1];
  var c = addr.match(/([^\s,，、]+?市)/g);
  if (c && c.length >= 2) return c[c.length - 2];
  if (c && c.length === 1) return c[0];
  return addr.length > 6 ? addr.substring(0, 6) + '...' : addr;
}
function getDataList(res) {
  if (!res) return null;
  if (res.data && Array.isArray(res.data)) return res.data;
  if (res.content && res.content.data && Array.isArray(res.content.data)) return res.content.data;
  return null;
}
function tsStart(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}
function tsEnd(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate(), 23, 59, 59, 999).getTime();
}
function projDateFilter(dateField, projField, start, end, projName) {
  var o = {};
  o[dateField] = [tsStart(start), tsEnd(end)];
  o[projField] = projName;
  return JSON.stringify(o);
}
export function didMount() {
  var S = this;
  S.utils.yida.searchFormDatas({
    formUuid: FU.project,
    pageSize: 100,
    currentPage: 1
  }).then(function (r) {
    var list = getDataList(r);
    if (!list) {
      _L = false;
      S.setState({
        t: Date.now()
      });
      return;
    }
    var ps = [];
    for (var i = 0; i < list.length; i++) {
      var fd = list[i].formData;
      if (!fd) continue;
      var nm = ev(fd, F.pName) || '';
      if (nm.indexOf('测试') > -1 || nm.toLowerCase().indexOf('test') > -1) continue;
      ps.push({
        instId: list[i].formInstId || '',
        name: nm,
        director: ev(fd, F.pDirector) || '-',
        area: extractDistrict(ev(fd, F.pAddr))
      });
    }
    _P = ps;
    if (ps.length === 0) {
      _L = false;
      S.setState({
        t: Date.now()
      });
      return;
    }
    S.loadStats('week');
  }).catch(function () {
    _L = false;
    S.setState({
      t: Date.now()
    });
  });
}
export function loadStats(range) {
  var S = this;
  _L = true;
  _stats = {};
  S.setState({
    t: Date.now()
  });
  var rg = getRange(range);
  var start = rg.s,
    end = rg.e;
  var results = {};
  var projIdx = 0;
  function processNext() {
    if (projIdx >= _P.length) {
      _stats = results;
      _L = false;
      S.setState({
        t: Date.now()
      });
      return;
    }
    var proj = _P[projIdx];
    projIdx++;
    var batch = [];
    SRCS.forEach(src => {
      batch.push(S.utils.yida.searchFormDatas({
        formUuid: src.u,
        pageSize: 1,
        currentPage: 1,
        searchFieldJson: projDateFilter(src.d, src.f, start, end, proj.name)
      }).then(function (res) {
        if (!results[proj.name]) results[proj.name] = {};
        var tc = 0;
        if (res && typeof res.totalCount === 'number') tc = res.totalCount;else if (res && res.content && typeof res.content.totalCount === 'number') tc = res.content.totalCount;
        results[proj.name][src.k] = tc;
      }).catch(function () {
        if (!results[proj.name]) results[proj.name] = {};
        results[proj.name][src.k] = 0;
      }));
    });
    Promise.all(batch).then(function () {
      processNext();
    });
  }
  processNext();
}
export function switchTime(e) {
  _R = e.target.dataset.range;
  this.loadStats(_R);
}
export function goProject(instId, name) {
  this.utils.router.push('FORM-2CF7E8C93FB34BA28B9566BC8E9EDF632HZA', {
    projId: instId,
    projName: name
  }, false);
}

// 时间标签
function rangeLabel(r) {
  var n = new Date();
  if (r === 'week') {
    var s = ws(n),
      e = we(n);
    return '第' + iw(n) + '周（' + fd(s) + '~' + fd(e) + '）';
  }
  if (r === 'lastWeek') {
    var s = lws(n),
      e = lwe(n);
    return '第' + (iw(n) - 1) + '周（' + fd(s) + '~' + fd(e) + '）';
  }
  if (r === 'month') return n.getFullYear() + '年' + (n.getMonth() + 1) + '月';
  var pm = new Date(n.getFullYear(), n.getMonth() - 1, 1);
  return pm.getFullYear() + '年' + (pm.getMonth() + 1) + '月';
}
function periodName(r) {
  return r === 'week' ? '本周' : r === 'lastWeek' ? '上周' : r === 'month' ? '本月' : '上月';
}
export function renderJsx() {
  var S = this;
  var t = this.state && this.state.t;
  var isM = this.utils.isMobile();
  var rL = rangeLabel(_R);
  var pL = periodName(_R);
  var totR = 0;
  _P.forEach(p => {
    var s = _stats[p.name] || {};
    SRCS.forEach(sk => {
      totR += s[sk.k] || 0;
    });
  });
  var Sty = {
    ct: {
      padding: isM ? '10px 12px' : '16px 20px',
      maxWidth: '1200px',
      margin: '0 auto',
      fontFamily: '-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif',
      borderRadius: '0 !important',
      background: '#f0f2f5',
      minHeight: '100vh'
    },
    hd: {
      background: 'linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%)',
      borderRadius: '12px',
      padding: isM ? '16px 16px' : '22px 26px',
      color: '#fff',
      marginBottom: '18px'
    },
    cd: {
      background: '#fff',
      borderRadius: '10px',
      padding: isM ? '14px 14px' : '18px 22px',
      marginBottom: '16px',
      boxShadow: '0 1px 3px rgba(0,0,0,0.06)'
    },
    st2: {
      fontSize: '15px',
      fontWeight: 600,
      color: '#1a1a2e',
      marginBottom: '14px',
      paddingBottom: '8px',
      borderBottom: '2px solid #eef2ff',
      display: 'flex',
      alignItems: 'center',
      gap: '8px'
    },
    sn: {
      display: 'inline-flex',
      width: '24px',
      height: '24px',
      background: 'linear-gradient(135deg,#3b82f6,#2563eb)',
      color: '#fff',
      borderRadius: '6px',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '12px',
      fontWeight: 700,
      flexShrink: 0
    },
    sg: {
      display: 'flex',
      gap: '14px',
      flexWrap: 'wrap'
    },
    si: {
      flex: '1 1 160px',
      textAlign: 'center',
      padding: '16px',
      background: '#fafbfc',
      borderRadius: '8px',
      border: '1px solid #f0f0f0'
    },
    sl: {
      fontSize: '12px',
      color: '#8c8c8c',
      marginBottom: '6px'
    },
    sv: {
      fontSize: '24px',
      fontWeight: 700
    },
    tb: {
      width: '100%',
      borderCollapse: 'collapse',
      fontSize: '12px',
      border: '1px solid #f0f0f0',
      borderRadius: '8px',
      overflow: 'hidden'
    },
    th: {
      padding: isM ? '5px 6px' : '8px 10px',
      textAlign: 'center',
      fontWeight: 600,
      color: '#595959',
      fontSize: '11px',
      background: '#f8f9fb',
      borderBottom: '1px solid #f0f0f0'
    },
    td: {
      padding: isM ? '5px 6px' : '7px 10px',
      textAlign: 'center',
      borderBottom: '1px solid #f5f5f5',
      color: '#262626'
    },
    pb: {
      background: '#fafbfc',
      border: '1px solid #f0f0f0',
      borderRadius: '8px',
      marginBottom: '10px',
      overflow: 'hidden'
    },
    ph: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '8px 14px',
      background: '#fff',
      borderBottom: '1px solid #f0f0f0'
    },
    sr: {
      display: 'flex',
      gap: '6px',
      padding: '10px 14px',
      flexWrap: 'wrap'
    },
    sc: {
      flex: '1 1 60px',
      textAlign: 'center',
      padding: '6px 4px',
      background: '#fff',
      borderRadius: '4px',
      border: '1px solid #f0f0f0',
      fontSize: '11px'
    },
    lnk: {
      color: '#4f46e5',
      textDecoration: 'none',
      cursor: 'pointer',
      fontWeight: 500
    },
    rt: {
      display: 'inline-block',
      fontSize: '10px',
      padding: '1px 6px',
      borderRadius: '3px',
      background: '#ecfdf5',
      color: '#059669',
      fontWeight: 400,
      whiteSpace: 'nowrap'
    },
    btn: {
      border: 'none',
      background: 'rgba(255,255,255,0.1)',
      color: 'rgba(255,255,255,0.6)',
      padding: '5px 10px',
      borderRadius: '6px',
      cursor: 'pointer',
      fontSize: '12px'
    },
    btnA: {
      border: 'none',
      background: 'rgba(255,255,255,0.2)',
      color: '#fff',
      padding: '5px 10px',
      borderRadius: '6px',
      cursor: 'pointer',
      fontSize: '12px'
    }
  };
  function isA(r) {
    return _R === r ? Sty.btnA : Sty.btn;
  }
  return <div style={Sty.ct}><div style={{
      display: "none"
    }}>{this.state && this.state.timestamp}</div>
      <div style={{
      display: 'none'
    }}>{t}</div>
      <div style={Sty.hd}>
        <div style={{
        fontSize: '11px',
        background: 'rgba(255,255,255,0.15)',
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: '20px',
        marginBottom: '6px'
      }}>📊 平台运营报告</div>
        <div style={{
        fontSize: isM ? '15px' : '18px',
        fontWeight: 700,
        letterSpacing: '0.5px'
      }}>【平台运营报告-{_R.indexOf('week') > -1 ? '周报' : '月报'}-{rL}】</div>
        <div style={{
        fontSize: '12px',
        color: 'rgba(255,255,255,0.6)',
        marginTop: '4px'
      }}>报告生成时间：{fd(new Date())}</div>
        <div style={{
        marginTop: '12px',
        display: 'flex',
        gap: '4px',
        flexWrap: 'wrap'
      }}>
          <button style={isA('week')} data-range="week" onClick={function (e) {
          S.switchTime(e);
        }}>本周</button>
          <button style={isA('lastWeek')} data-range="lastWeek" onClick={function (e) {
          S.switchTime(e);
        }}>上周</button>
          <button style={isA('month')} data-range="month" onClick={function (e) {
          S.switchTime(e);
        }}>本月</button>
          <button style={isA('lastMonth')} data-range="lastMonth" onClick={function (e) {
          S.switchTime(e);
        }}>上月</button>
        </div>
      </div>

      <div style={Sty.cd}>
        <div style={Sty.st2}><span style={Sty.sn}>1</span>平台基本运营数据</div>
        <div style={Sty.sg}>
          <div style={Sty.si}><div style={Sty.sl}>📁 报告时间</div><div style={Object.assign({}, Sty.sv, {
            fontSize: '16px',
            color: '#3b82f6'
          })}>{rL}</div></div>
          <div style={Sty.si}><div style={Sty.sl}>🏗️ 项目数量</div><div style={Object.assign({}, Sty.sv, {
            color: '#10b981'
          })}>{_P.length}<span style={{
              fontSize: '13px',
              fontWeight: 400,
              color: '#8c8c8c'
            }}> 个</span></div></div>
          <div style={Sty.si}><div style={Sty.sl}>📝 {pL}总记录</div><div style={Object.assign({}, Sty.sv, {
            color: '#f59e0b'
          })}>{totR}<span style={{
              fontSize: '13px',
              fontWeight: 400,
              color: '#8c8c8c'
            }}> 条</span></div></div>
        </div>
      </div>

      <div style={Sty.cd}>
        <div style={Sty.st2}><span style={Sty.sn}>2</span>项目基本信息</div>
        {_L ? <div style={{
        padding: '20px',
        color: '#8c8c8c',
        textAlign: 'center'
      }}>统计中...</div> : _P.length === 0 ? <div style={{
        padding: '20px',
        color: '#8c8c8c',
        textAlign: 'center'
      }}>暂无项目数据</div> : <div style={{
        overflowX: 'auto'
      }}><table style={Sty.tb}>
          <thead><tr>
            <th style={Object.assign({}, Sty.th, {
                textAlign: 'left'
              })}>项目名称</th>
            <th style={Sty.th}>负责人</th><th style={Sty.th}>项目总监</th><th style={Sty.th}>区域</th><th style={Sty.th}>{pL}总记录</th>
          </tr></thead>
          <tbody>{_P.map((p, idx) => {
              var s = _stats[p.name] || {};
              var tot = 0;
              SRCS.forEach(sk => {
                tot += s[sk.k] || 0;
              });
              return <tr key={idx}>
              <td style={Object.assign({}, Sty.td, {
                  textAlign: 'left',
                  fontWeight: 500
                })}><a style={Sty.lnk} onClick={function () {
                    S.goProject(p.instId, p.name);
                  }}>{p.name}</a></td>
              <td style={Sty.td}>-</td>
              <td style={Sty.td}><span style={Object.assign({}, Sty.rt, {
                    background: '#fff7ed',
                    color: '#d97706'
                  })}>{p.director}</span></td>
              <td style={Sty.td}>{p.area}</td>
              <td style={Sty.td}><strong style={{
                    color: '#3b82f6',
                    fontSize: '14px'
                  }}>{tot}</strong></td>
            </tr>;
            })}</tbody>
        </table></div>}
      </div>

      <div style={Sty.cd}>
        <div style={Sty.st2}><span style={Sty.sn}>3</span>项目清单 · 数据统计</div>
        {_L ? <div style={{
        padding: '20px',
        color: '#8c8c8c',
        textAlign: 'center'
      }}>统计中...</div> : _P.length === 0 ? <div style={{
        padding: '20px',
        color: '#8c8c8c',
        textAlign: 'center'
      }}>暂无项目数据</div> : _P.map((p, idx) => {
        var s = _stats[p.name] || {};
        var tot = 0;
        SRCS.forEach(sk => {
          tot += s[sk.k] || 0;
        });
        return <div key={idx} style={Sty.pb}>
            <div style={Sty.ph}><div><span style={{
                fontSize: '12px',
                fontWeight: 600
              }}>{p.name}</span></div><div style={{
              fontSize: '11px',
              color: '#8c8c8c'
            }}>{pL} <strong style={{
                color: '#1a1a2e'
              }}>{tot}</strong> 条</div></div>
            <div style={Sty.sr}>{SRCS.map((sk, si) => {
              var cnt = s[sk.k] || 0;
              return <div key={si} style={Sty.sc}><span style={{
                  color: '#8c8c8c'
                }}>{sk.l}</span> <span style={{
                  fontWeight: 700,
                  color: sk.c
                }}>{cnt}</span></div>;
            })}</div>
          </div>;
      })}
      </div>
    </div>;
}