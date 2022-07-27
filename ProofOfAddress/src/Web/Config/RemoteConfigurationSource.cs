namespace MyLocalFarmer.ProofOfAddress.Web.Config
{
    public class RemoteConfigurationSource : IConfigurationSource
    {
        private readonly HttpClient _httpClient;
        
        public RemoteConfigurationSource(HttpClient httpClient)
        {
            _httpClient = httpClient;
        }

        public IConfigurationProvider Build(IConfigurationBuilder builder)
        {
            
            return new RemoteConfigurationProvider(_httpClient);
        }
    }
}
