/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.acme;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieRuntimeBuilder;
import org.kie.api.runtime.KieSession;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

public class LoanApplicationEndPoint {

	private final class RuleListener extends DefaultAgendaEventListener {
		private List<String> matchedRules = new ArrayList<>();

		private RuleListener() {
		}


		@Override
		public void afterMatchFired(AfterMatchFiredEvent event) {
			matchedRules.add(event.getMatch().getRule().getName());
			
		}

		public List<String> getMatchedRule() {
			return matchedRules;
		}
	}

	@Inject
	KieRuntimeBuilder kieRuntimeBuilder;

    @Tool(description = "Requires approval for a loan")
	public String approveLoan(
			@ToolArg(description = "The name of the applicant requesting the loan") String name,
			@ToolArg(description = "The age of the applicant requesting the loan") int age,
			@ToolArg(description = "The amount of the loan requested") int amount, 
			@ToolArg(description = "The amount of the deposit the applicant has") int deposit) {
		KieSession session = kieRuntimeBuilder.newKieSession();
		Applicant applicant = new Applicant(name, age);
		LoanApplication loanApplication = new LoanApplication(applicant, amount, deposit);

		List<LoanApplication> approvedApplications = new ArrayList<>();
		session.setGlobal("approvedApplications", approvedApplications);
		session.setGlobal("maxAmount", 1000);

		session.insert(loanApplication);
		
		RuleListener ruleListener = new RuleListener();
		session.addEventListener( ruleListener);

		session.fireAllRules();
		session.dispose();
		
		if (approvedApplications.isEmpty()) {
			return "None approved";
		} else {
			return "Approved loan of "+ approvedApplications.get(0).getAmount() + " for " + approvedApplications.get(0).getApplicant().getName()
					+ "\n The rule used to approve the loan is " + ruleListener.getMatchedRule();
		}
		
	}
}
