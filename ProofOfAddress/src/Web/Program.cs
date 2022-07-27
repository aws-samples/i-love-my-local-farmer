using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.AspNetCore.Components.Web;
using Microsoft.AspNetCore.Components.WebAssembly.Hosting;
using MyLocalFarmer.ProofOfAddress.Web;
using MyLocalFarmer.ProofOfAddress.Web.Config;
using MyLocalFarmer.ProofOfAddress.Web.Providers;
using MyLocalFarmer.ProofOfAddress.Web.Services;

var builder = WebAssemblyHostBuilder.CreateDefault(args);

builder.RootComponents.Add<App>("#app");
builder.RootComponents.Add<HeadOutlet>("head::after");

builder.Services.AddAuthorizationCore();

// add our custom authentication state provider in the Dependency Injection container
builder.Services.AddScoped<BFFAuthenticationStateProvider>();

// configure our custom authentication state provider to be server as the default AuthenticationStateProvider
builder.Services.AddScoped<AuthenticationStateProvider, BFFAuthenticationStateProvider>();

// add a ICurrentUserService implementation in the Dependency Injection container
builder.Services.AddScoped<ICurrentUserService, BFFCurrentUserService>();

// configure the default HttpClient to use the host environment based address as the base address for each request
builder.Services.AddScoped(sp => new HttpClient { BaseAddress = new Uri(builder.HostEnvironment.BaseAddress) });

// try to dynamically load the application configuration from the an endpoint at the "config" relative path
HttpClient httpClient = builder.Services.BuildServiceProvider().GetRequiredService<HttpClient>();
try
{
    builder.Configuration.AddJsonStream(await httpClient.GetStreamAsync("config"));
}
catch
{
    Console.WriteLine($"Unable to load remote configuration at {builder.HostEnvironment.BaseAddress + "config"}");
}

await builder.Build().RunAsync();
