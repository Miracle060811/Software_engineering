# -*- coding: utf-8 -*-
import re
p = r"E:\SoftEngneeringHomework\Software_engineering\defense_ppt\slides\slide-16.xml"
s = open(p, encoding="utf-8").read()

# 1) remove colSpan/rowSpan from <content>
s = s.replace(' textAlign="center" colSpan="1">', ' textAlign="center">')
s = s.replace(' textAlign="left" colSpan="2">', ' textAlign="left">')
s = s.replace(' textAlign="center" rowSpan="2">', ' textAlign="center">')

# 2) left table last row: second td spans 2 columns
s = s.replace(
    '<td><fill><fillColor color="rgba(247,249,252,1)"/></fill><content textType="caption" fontSize="9" color="rgba(46,52,60,1)" textAlign="left"><p>　同提交',
    '<td colSpan="2"><fill><fillColor color="rgba(247,249,252,1)"/></fill><content textType="caption" fontSize="9" color="rgba(46,52,60,1)" textAlign="left"><p>　同提交')

# 3) right table: scenario-name td spans 2 rows
def add_rowspan(m):
    return '<td rowSpan="2">' + m.group(1)

pat = re.compile(r'<td>(<fill><fillColor color="rgba\(255,255,255,1\)"/></fill><content textType="caption" fontSize="9" color="rgba\(46,52,60,1\)" textAlign="center"><p>(?:航班查询|酒店查询|并发下单)</p>)')
s = pat.sub(add_rowspan, s)

open(p, "w", encoding="utf-8").write(s)
print("fixed; rowSpan count:", s.count('rowSpan="2"'), "colSpan:", s.count('colSpan="2"'))
