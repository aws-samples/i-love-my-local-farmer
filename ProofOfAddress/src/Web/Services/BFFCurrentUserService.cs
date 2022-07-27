using Microsoft.AspNetCore.Components.WebAssembly.Http;
using MyLocalFarmer.ProofOfAddress.Shared;
using System.Net.Http.Json;

namespace MyLocalFarmer.ProofOfAddress.Web.Services
{
    public class BFFCurrentUserService : ICurrentUserService
    {
        private readonly HttpClient _httpClient;
        private readonly IConfiguration _configuration;
        
        public BFFCurrentUserService(HttpClient httpClient, IConfiguration configuration)
        {
            _httpClient = httpClient;
            _configuration = configuration;
        }

        public async Task<CurrentUser> GetCurrentUser()
        {
            var request = new HttpRequestMessage()
            {
                Method = HttpMethod.Get,
                RequestUri = new Uri(_configuration[nameof(Config.RemoteConfigurationProvider.RemoteConfiguration.GetCurrentUserUrl)])
            };

            request.SetBrowserRequestCredentials(BrowserRequestCredentials.Include);

            var response = await _httpClient.SendAsync(request);
            var result = await response.Content.ReadFromJsonAsync<CurrentUser>();
            return result ?? new CurrentUser(false, string.Empty, new Dictionary<string, string>());
        }
    }
}
