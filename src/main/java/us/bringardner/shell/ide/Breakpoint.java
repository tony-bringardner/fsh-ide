/**
*	Copyright 2024 Tony Bringardner
*
*	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
*	You may obtain a copy of the License at
*
*	http://www.apache.org/licenses/LICENSE-2.0
*
*	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
*	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for 
*	the specific language governing permissions and limitations under the License.
*/
package us.bringardner.shell.ide;

import javax.swing.text.Highlighter.Highlight;

public class Breakpoint {
	private static int instanceCount=-1;
	private int id;
	private Highlight tag;
	private boolean enabled=true;
	private int line=-1;
	private String code="";
	private boolean isConditional=false;
	private String condition="";
	private int targetHitCount=-1;
	private int actualHitCount = -1;
	private boolean isHitCount=false;
	
	
	public Breakpoint() {
		synchronized (Breakpoint.class) {
			id = ++instanceCount;	
		}		
	}
	
	public Breakpoint(Highlight tag) {
		this();
		this.tag = tag;
	}
	
	public Highlight getTag() {
		return tag;
	}


	public void setTag(Highlight tag) {
		this.tag = tag;
	}


	public int getId() {
		return id;
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public boolean isConditional() {
		return isConditional;
	}
	public void setConditional(boolean isConditional) {
		this.isConditional = isConditional;
	}
	public boolean isHitCount() {
		return isHitCount;
	}
	public void setHitCount(boolean isHitCount) {
		this.isHitCount = isHitCount;
	}
	public boolean isEnabled(boolean incHitCount) {
		boolean ret = enabled;
		if( enabled && incHitCount && targetHitCount>0) {
			ret = ++actualHitCount == targetHitCount;
		}
		
		return ret;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public int getOffset() {
		return tag.getStartOffset();
	}
	
	public int getLine() {
		return line;
	}
	public void setLine(int line) {
		this.line = line;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public int getHitCount() {
		return targetHitCount;
	}
	public void setHitCount(int hitCount) {
		this.targetHitCount = hitCount;
	}
	
	public void reset() {
		actualHitCount = 0;		
	}
	
}
