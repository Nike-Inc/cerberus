/// <reference types="Cypress" />

const delay = 3000

describe("Cerberus Dashboard: Visual Regression Tests (End-to-End)", () => {
  // beforeEach(() => cy.visit(Cypress.appConfig("baseUrl")));

  context.skip("Okta", () => {
    before(() => cy.visit(Cypress.config("baseUrl")));
    Cypress.on('uncaught:exception', (err, runnable) => false)

    it("Should Login", () => {
      let options = {args: {delay, email: Cypress.env("auth_username"), password: Cypress.env("auth_password")}}
      cy.origin('nike-qa.oktapreview.com', options, ({delay, email, password}) => {
        cy.get('input:first')
        cy.get("main").should("exist")
        cy.get('input#okta-signin-username').type(email, {force: true}).wait(delay);
        cy.get('[id=okta-signin-password]').type(password, {force: true}).wait(delay);
        cy.get('#okta-signin-submit').click().wait(delay);
      })
    })

    it("Should work", () => {
      cy.get('[id=root]').within(() => {
        cy.get('[id=content-wrapper]').within(($contentWrapper) => {
          cy.get('[id=content]').should('have.length', 1)
          cy.get('[id=content]').children().should('have.length', 2);
        })
      }).wait(delay);
    })

    it("Should Logout", () => {
      cy.get("[id=u-b-name]").click().wait(delay);
      cy.get(".context-menu-button").last().click().wait(delay);
    });

    // after(() => cy.visit(Cypress.appConfig("baseUrl")));
  });

  context("Admin", () => {
    before(() => cy.visit(Cypress.config("baseUrl")));
    after(() => cy.visit(Cypress.config("baseUrl")));
    Cypress.on('uncaught:exception', (err, runnable) => false)

    it("Should login", () => {
      cy.get("input[name=\"username\"]").type(Cypress.env("cerberus_test_username"), {force: true});
      cy.get("input[name=\"password\"]").type(Cypress.env("cerberus_test_password"), {force: true});
      cy.get("#login-btn").click().wait(delay);
    });

    it.skip("Should have access to the Redux store", () => {
      cy.window()
        .its("store")
        .invoke("getState")
        .should("have.property", "auth")
        .and("not.be.undefined")
        .and("not.be.NaN")
        .and("not.be.null");
    });

    it.skip("Should be accessible if authenticated, authenticate if not accessible", () => {
      cy.window()
        .its("store")
        .invoke("getState")
        .its("auth")
        .its("isAuthenticated")
        .then((isAuthenticated) => cy.skipOn(isAuthenticated));

      cy.get("input[name=\"username\"]").type(Cypress.env("cerberus_test_username"), {force: true});
      cy.get("input[name=\"password\"]").type(Cypress.env("cerberus_test_password"), {force: true});
      cy.get("#login-btn").click().wait(delay);

      cy.window()
        .its("store")
        .invoke("getState")
        .its("auth")
        .its("isAuthenticated")
        .should("be.true");
    });

    it.skip("render SDB Summary Model", () => {
      cy.get(".app-messenger-wrapper").its("length").should("eq", 0);
      cy.get("#u-b-name").click();
      cy.get(".context-menu-button").first().click();
      cy.get("#app-messenger-wrapper").its("length").should("eq", 0);
    });

    it.skip("render View Token Model", () => {
      cy.get("#u-b-name").click();
      cy.get(".context-menu-button").eq(1).click();
    });

    it.skip("render View Token Model and show token", () => {
      cy.get("#u-b-name").click();
      cy.get(".context-menu-button:nth-child(2)").click();
      cy.get(".row-btn-revealed").click();
    });

    it.skip("update an SDB", () => {
      cy.get(":nth-child(3) > .ncss-brand").click();
      cy.get(".setting").click();
      cy.get(".sdb-settings-name > .sdb-settings-value").invoke("text").then((text) => {
        cy.get(".ncss-btn-dark-grey").click();
        // TODO: actually edit the SDB
        cy.get("#submit-btn").click();
      });
    });

    it.skip("delete an SDB", () => {
      cy.get(":nth-child(3) > .ncss-brand").click();
      cy.get(".setting").click();
      cy.get(".sdb-settings-name > .sdb-settings-value").invoke("text").then((text) => {
        cy.get(".ncss-btn-accent").click();
        cy.get(".modal-component-wrapper").within(() => {
          cy.get("input[type='text']").type(text, {force: true});
          cy.get("#submit-btn").click();
        });
      });
    });

    it("logout", () => {
      cy.get("#u-b-name").click();
      cy.get(".context-menu-button").last().click();
    });
  });

  // afterEach(() => cy.visit(Cypress.appConfig("baseUrl")));
});
