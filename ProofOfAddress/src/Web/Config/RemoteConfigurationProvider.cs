using Microsoft.Extensions.Primitives;
using System.Net.Http.Json;

namespace MyLocalFarmer.ProofOfAddress.Web.Config
{
    public class RemoteConfigurationProvider : ConfigurationProvider
    {
        public record RemoteConfiguration
        {
            public string GetPresignedUrl { get; init; } = default!;
            public string LoginUrl { get; init; } = default!;
            public string LogoutUrl { get; init; } = default!;
            public string GetCurrentUserUrl { get; init; } = default!;
        }

        private HttpClient _httpClient;
        

        public RemoteConfigurationProvider(HttpClient httpClient)
        {
            _httpClient = httpClient;
        }

        public override void Load()
        {
            try
            {
                var response = _httpClient.GetFromJsonAsync<RemoteConfiguration>(_httpClient.BaseAddress + "config");
                response.Wait();
                
                Data.Add(nameof(RemoteConfiguration.GetPresignedUrl), response.Result?.GetPresignedUrl);
                Data.Add(nameof(RemoteConfiguration.LoginUrl), response.Result?.LoginUrl);
                Data.Add(nameof(RemoteConfiguration.LogoutUrl), response.Result?.LogoutUrl);
                Data.Add(nameof(RemoteConfiguration.GetCurrentUserUrl), response.Result?.GetCurrentUserUrl);
            }
            catch(Exception e)
            {
                Console.WriteLine("Error while retrieving configuration, no remote configuration loaded");
                Console.WriteLine(e.Message);
            }
        }
    }
}
