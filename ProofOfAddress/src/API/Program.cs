using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.IdentityModel.Protocols.OpenIdConnect;
using System.Text;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders =
        ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;
    options.KnownNetworks.Clear();
    options.KnownProxies.Clear();
});

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = CookieAuthenticationDefaults.AuthenticationScheme;
    options.DefaultSignInScheme = CookieAuthenticationDefaults.AuthenticationScheme;
    options.DefaultSignOutScheme = CookieAuthenticationDefaults.AuthenticationScheme;
})
    // Configure cookie based authentication between the Blazor WebAssembly app and the BFF API
    .AddCookie(o =>
    {
        o.Cookie.SecurePolicy = CookieSecurePolicy.Always;
        o.Cookie.SameSite = SameSiteMode.None;
        o.Cookie.HttpOnly = true;
    })
    // Configure the OpenID Connect based authentication between the BFF API and Amazon Cognito
    .AddOpenIdConnect("Cognito", options =>
    {
        options.Authority = builder.Configuration["Authority"];
        options.ClientId = builder.Configuration["ClientId"];
        options.ClientSecret = builder.Configuration["ClientSecret"];
        options.ResponseType = OpenIdConnectResponseType.Code;
        options.SaveTokens = true;
        options.Scope.Clear();
        options.Scope.Add("openid");
        options.CallbackPath = "/callback";
        // Handle the OnRedirectToIdentityProviderForSignOut event as Amazon Cognito has a specific logout action
        options.Events.OnRedirectToIdentityProviderForSignOut = context =>
        {
            StringBuilder logoutUri = new StringBuilder(builder.Configuration["LogoutUri"]);
            logoutUri.Append($"?client_id={context.Options.ClientId}");
            if (context.Request.Query.ContainsKey("returnUrl"))
            {
                logoutUri.Append($"&logout_uri={context.Request.Query["returnUrl"]}");
            }
            context.Response.Redirect(logoutUri.ToString().TrimEnd('/'));
            context.HandleResponse();
            return Task.CompletedTask;
        };
    });

builder.Services.AddControllers();

builder.Services.AddLogging();

var app = builder.Build();

// Configure the HTTP request pipeline.
app.UseForwardedHeaders();

app.UseHttpsRedirection();

app.UseAuthentication();

app.UseAuthorization();

// Configure CORS
app.UseCors(policy =>
    policy.WithOrigins(builder.Configuration["ClientOrigin"])
        .AllowCredentials());

app.MapControllers();

app.Run();
