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

import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.autocomplete.TemplateCompletion;




public class Template {
	private String name="";
	private String description="";
	private String code="";

	public Template(String name, String description, String code) {
		this.name = name;
		this.description = description;
		this.code = code;
	}

	public Template() {
	}

	public Template(String name, String code) {
		this.name = name;
		this.code = code;
	}


	public void addCompetion(DefaultCompletionProvider provider) {
		String d = description == null ? name : description.isBlank() ? name : description;

		if( code == null || code.isBlank()) {
			provider.addCompletion(new ShorthandCompletion(provider,name,name,d));
		} else if( code.indexOf("$") >= 0) {
			provider.addCompletion(new TemplateCompletion(provider, name,d,code));
		} else {
			provider.addCompletion(new ShorthandCompletion(provider,name,code,d));
		}
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	public Template copy() {
		return new Template(name, description, code);
	}

	@Override
	public boolean equals(Object obj) {
		boolean ret = false;
		if (obj instanceof Template) {
			Template tp = (Template) obj;
			ret = tp.name.equals(name) 
					&& tp.description.equals(description)
					&& tp.code.equals(code)
					;
		}
		return ret;
	}


}
