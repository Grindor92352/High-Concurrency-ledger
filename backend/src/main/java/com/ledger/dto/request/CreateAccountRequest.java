package com.ledger.dto.request;

/** Empty today — account creation only needs the authenticated caller's identity,
 *  taken from the JWT rather than the request body. Kept as a class so the
 *  POST endpoint has a body to extend later (e.g. account nickname, currency). */
public record CreateAccountRequest() {
}
